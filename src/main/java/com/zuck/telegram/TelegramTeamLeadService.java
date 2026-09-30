package com.zuck.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zuck.agent.AgentDefinition;
import com.zuck.llm.LlmClient;
import com.zuck.llm.TeamDiscussion;
import com.zuck.project.Project;
import com.zuck.team.TeamCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Service
public class TelegramTeamLeadService {

    private static final Logger log = LoggerFactory.getLogger(TelegramTeamLeadService.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final TeamCoordinator coordinator;
    private final LlmClient llmClient;
    private final String token;
    private final long allowedUserId;
    private final long allowedChatId;
    private final boolean enabled;
    private final boolean naturalLanguageEnabled;

    private long updateOffset = 0;

    public TelegramTeamLeadService(
            ObjectMapper objectMapper,
            TeamCoordinator coordinator,
            LlmClient llmClient,
            @Value("${telegram.bot.token:}") String token,
            @Value("${telegram.bot.allowed-user-id:0}") long allowedUserId,
            @Value("${telegram.bot.allowed-chat-id:0}") long allowedChatId,
            @Value("${telegram.bot.enabled:true}") boolean enabled,
            @Value("${telegram.bot.natural-language:true}") boolean naturalLanguageEnabled) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.coordinator = coordinator;
        this.llmClient = llmClient;
        this.token = token;
        this.allowedUserId = allowedUserId;
        this.allowedChatId = allowedChatId;
        this.enabled = enabled;
        this.naturalLanguageEnabled = naturalLanguageEnabled;
    }

    @Scheduled(fixedDelayString = "${telegram.bot.polling-delay-ms:1000}")
    public void pollUpdates() {
        if (!enabled || token.isBlank()) {
            return;
        }

        try {
            String url = "https://api.telegram.org/bot" + token
                    + "/getUpdates?timeout=10&limit=20&offset=" + updateOffset;

            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            JsonNode root = objectMapper.readTree(response.body());
            if (!root.path("ok").asBoolean(false)) {
                log.warn("Telegram getUpdates failed: {}", root.path("description").asText());
                return;
            }

            for (JsonNode update : root.path("result")) {
                updateOffset = update.path("update_id").asLong() + 1;
                handleUpdate(update);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Telegram polling failed", e);
        }
    }

    public void handleUpdate(JsonNode update) throws Exception {
        JsonNode message = update.path("message");
        if (message.isMissingNode()) {
            return;
        }

        long chatId = message.path("chat").path("id").asLong();
        long userId = message.path("from").path("id").asLong();

        if (allowedChatId != 0 && allowedChatId != chatId) {
            return;
        }

        if (allowedUserId != 0 && allowedUserId != userId) {
            sendMessage(chatId, "I received your message, but Zuck is not configured for your user ID yet. "
                    + "Send /whoami, then set TELEGRAM_ALLOWED_USER_ID=" + userId);
            return;
        }

        // 1. Handle Spoken Voice or Audio Note
        if (message.has("voice") || message.has("audio")) {
            handleVoiceOrAudioMessage(chatId, message);
            return;
        }

        // 2. Handle Text Messages
        if (!message.has("text")) {
            return;
        }

        String text = message.path("text").asText().trim();

        // Strip surrounding quotes if user copied/pasted with quotes
        if ((text.startsWith("\"") && text.endsWith("\"")) || (text.startsWith("'") && text.endsWith("'"))) {
            text = text.substring(1, text.length() - 1).trim();
        }

        if (text.equals("/whoami")) {
            sendMessage(chatId, "Your Telegram user ID is: " + userId + "\nChat ID is: " + chatId);
            return;
        }

        if (text.equals("/start") || text.equals("/help")) {
            sendHelp(chatId);
            return;
        }

        if (text.equals("/team")) {
            sendTeamRoster(chatId);
            return;
        }

        if (text.equals("/project") || text.equals("/current")) {
            sendCurrentProject(chatId);
            return;
        }

        if (text.equals("/projects")) {
            sendProjectsList(chatId);
            return;
        }

        if (text.startsWith("/switch ")) {
            String projectId = text.substring(8).trim();
            handleSwitchProject(chatId, projectId);
            return;
        }

        if (text.startsWith("/task ")) {
            coordinateAndDiscuss(chatId, text.substring(6).trim());
            return;
        }

        // Direct greeting/call
        String lower = text.toLowerCase();
        if (lower.equals("zuck") || lower.equals("@zuck") || lower.equals("hey zuck") || lower.equals("hi zuck")) {
            sendMessage(chatId, "🤖 Team Lead (Zuck) online! What engineering work should we tackle for " 
                    + coordinator.getCurrentProject().name() + "? Send a voice note or message with your task.");
            return;
        }

        // In group/channel: trigger if addressed to Zuck or natural language is enabled
        if (lower.startsWith("zuck") || lower.startsWith("@zuck") || (naturalLanguageEnabled && !text.startsWith("/"))) {
            String cleanedRequest = text.replaceFirst("(?i)^(zuck|@zuck)[:,\\s]*", "").trim();
            if (!cleanedRequest.isBlank()) {
                coordinateAndDiscuss(chatId, cleanedRequest);
            }
        }
    }

    private void handleVoiceOrAudioMessage(long chatId, JsonNode message) throws Exception {
        JsonNode audioNode = message.has("voice") ? message.path("voice") : message.path("audio");
        String fileId = audioNode.path("file_id").asText();
        String mimeType = audioNode.path("mime_type").asText("audio/ogg");

        if (fileId.isBlank()) {
            sendMessage(chatId, "Could not extract voice recording file from Telegram.");
            return;
        }

        sendMessage(chatId, "🎙️ Receiving audio note... Transcribing spoken request...");

        byte[] audioBytes = downloadTelegramFile(fileId);
        if (audioBytes == null || audioBytes.length == 0) {
            sendMessage(chatId, "❌ Failed to download audio file from Telegram.");
            return;
        }

        String transcription = llmClient.transcribeAudio(audioBytes, mimeType);
        if (transcription.isBlank() || transcription.startsWith("Audio transcription failed")) {
            sendMessage(chatId, "⚠️ " + transcription);
            return;
        }

        sendMessage(chatId, "🗣️ Transcribed Request:\n\"" + transcription + "\"");
        coordinateAndDiscuss(chatId, transcription);
    }

    private byte[] downloadTelegramFile(String fileId) {
        try {
            String getFileUrl = "https://api.telegram.org/bot" + token + "/getFile?file_id=" + fileId;
            HttpRequest getFileReq = HttpRequest.newBuilder(URI.create(getFileUrl)).GET().build();
            HttpResponse<String> getFileResp = httpClient.send(getFileReq, HttpResponse.BodyHandlers.ofString());

            JsonNode root = objectMapper.readTree(getFileResp.body());
            if (!root.path("ok").asBoolean(false)) {
                log.warn("Failed to getFile from Telegram: {}", root);
                return null;
            }

            String filePath = root.path("result").path("file_path").asText();
            String downloadUrl = "https://api.telegram.org/file/bot" + token + "/" + filePath;

            HttpRequest downloadReq = HttpRequest.newBuilder(URI.create(downloadUrl)).GET().build();
            HttpResponse<byte[]> downloadResp = httpClient.send(downloadReq, HttpResponse.BodyHandlers.ofByteArray());

            return downloadResp.body();
        } catch (Exception e) {
            log.error("Failed to download Telegram voice file", e);
            return null;
        }
    }

    private void coordinateAndDiscuss(long chatId, String task) throws Exception {
        if (task.isBlank()) {
            sendMessage(chatId, "Please describe the engineering work after /task or speak your request.");
            return;
        }

        TeamDiscussion discussion = coordinator.coordinateAndDiscuss(task);
        sendMessage(chatId, discussion.toFormattedTelegramMessage());
    }

    private void sendHelp(long chatId) throws Exception {
        sendMessage(chatId, """
                🤖 Zuck — AI Software Engineering Team Platform is online.

                Available Commands & Features:
                • Send Voice/Audio recording — Zuck transcribes your voice and convenes the team meeting!
                • Text "Zuck, let's build..." — Zuck automatically calls relevant engineers to discuss
                • /task <description> — bring engineering work to the team
                • /team — view all 8 members of the engineering team
                • /project — show currently active project context
                • /projects — list all registered projects
                • /switch <id> — switch active project context
                • /whoami — show your Telegram user/chat IDs
                • /help — show this message

                The team persists across projects. Mention the project name (e.g. "in habit-coach project") and Zuck will adapt automatically!
                """);
    }

    private void sendTeamRoster(long chatId) throws Exception {
        List<AgentDefinition> team = coordinator.getTeam();
        StringBuilder sb = new StringBuilder("👥 Persistent Software Engineering Team:\n\n");
        for (AgentDefinition agent : team) {
            sb.append("• ").append(agent.name())
              .append(" (").append(agent.role().name()).append(")\n")
              .append("  ").append(agent.description()).append("\n\n");
        }
        sendMessage(chatId, sb.toString().trim());
    }

    private void sendCurrentProject(long chatId) throws Exception {
        Project current = coordinator.getCurrentProject();
        sendMessage(chatId, String.format(
                "📂 Active Project Context:\n• ID: %s\n• Name: %s\n• Repo: %s\n• Branch: %s\n• Technologies: %s",
                current.id(),
                current.name(),
                current.repository(),
                current.defaultBranch(),
                String.join(", ", current.technologies())));
    }

    private void sendProjectsList(long chatId) throws Exception {
        List<Project> projects = coordinator.listProjects();
        StringBuilder sb = new StringBuilder("📚 Registered Projects:\n\n");
        String currentId = coordinator.getCurrentProject().id();
        for (Project p : projects) {
            String indicator = p.id().equals(currentId) ? "👉 [ACTIVE] " : "• ";
            sb.append(indicator).append(p.id()).append(" — ").append(p.name()).append("\n");
        }
        sb.append("\nUse /switch <projectId> to change active context.");
        sendMessage(chatId, sb.toString());
    }

    private void handleSwitchProject(long chatId, String projectId) throws Exception {
        try {
            coordinator.switchProject(projectId);
            Project project = coordinator.getCurrentProject();
            sendMessage(chatId, "✅ Switched active project context to: " + project.name() + " (" + project.id() + ")");
        } catch (IllegalArgumentException e) {
            sendMessage(chatId, "❌ " + e.getMessage() + ". Use /projects to view available IDs.");
        }
    }

    private void sendMessage(long chatId, String text) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("chat_id", chatId);
        body.put("text", text);

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("https://api.telegram.org/bot" + token + "/sendMessage"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
