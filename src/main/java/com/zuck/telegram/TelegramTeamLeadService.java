package com.zuck.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zuck.agent.AgentDefinition;
import com.zuck.project.Project;
import com.zuck.team.TeamCoordinationResult;
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
import java.util.List;

@Service
public class TelegramTeamLeadService {

    private static final Logger log = LoggerFactory.getLogger(TelegramTeamLeadService.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final TeamCoordinator coordinator;
    private final String token;
    private final long allowedUserId;
    private final long allowedChatId;
    private final boolean enabled;
    private final boolean naturalLanguageEnabled;

    private long updateOffset = 0;

    public TelegramTeamLeadService(
            ObjectMapper objectMapper,
            TeamCoordinator coordinator,
            @Value("${telegram.bot.token:}") String token,
            @Value("${telegram.bot.allowed-user-id:0}") long allowedUserId,
            @Value("${telegram.bot.allowed-chat-id:0}") long allowedChatId,
            @Value("${telegram.bot.enabled:true}") boolean enabled,
            @Value("${telegram.bot.natural-language:false}") boolean naturalLanguageEnabled) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
        this.coordinator = coordinator;
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
        if (message.isMissingNode() || !message.has("text")) {
            return;
        }

        long chatId = message.path("chat").path("id").asLong();
        long userId = message.path("from").path("id").asLong();
        String text = message.path("text").asText().trim();

        if (allowedChatId != 0 && allowedChatId != chatId) {
            return;
        }

        if (text.equals("/whoami")) {
            sendMessage(chatId, "Your Telegram user ID is: " + userId + "\nChat ID is: " + chatId);
            return;
        }

        if (allowedUserId != 0 && allowedUserId != userId) {
            sendMessage(chatId, "I received your message, but Zuck is not configured for your user ID yet. "
                    + "Send /whoami, then set TELEGRAM_ALLOWED_USER_ID=" + userId);
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
            coordinateTask(chatId, text.substring(6).trim());
            return;
        }

        if (naturalLanguageEnabled && !text.startsWith("/")) {
            coordinateTask(chatId, text);
        }
    }

    private void sendHelp(long chatId) throws Exception {
        sendMessage(chatId, """
                🤖 Zuck — AI Software Engineering Team Platform is online.

                Available Commands:
                /task <description> — bring engineering work to the team
                /team — view all 8 members of the engineering team
                /project — show currently active project context
                /projects — list all registered projects
                /switch <id> — switch active project context
                /whoami — show your Telegram user/chat IDs
                /help — show this message

                The team persists across projects. Switch target projects at any time.
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

    private void coordinateTask(long chatId, String task) throws Exception {
        if (task.isBlank()) {
            sendMessage(chatId, "Please describe the engineering work after /task.");
            return;
        }

        TeamCoordinationResult result = coordinator.coordinate(task);
        StringBuilder message = new StringBuilder()
                .append("📋 Team Lead (Zuck) received the request.\n\n")
                .append("Active Project: ").append(result.project().name()).append("\n")
                .append("Work status: ").append(result.workItem().status()).append("\n")
                .append("Relevant Participants: ");

        for (int i = 0; i < result.participants().size(); i++) {
            AgentDefinition agent = result.participants().get(i);
            if (i > 0) message.append(", ");
            message.append(agent.name());
        }

        message.append("\n\nNo code has been changed yet.");
        sendMessage(chatId, message.toString());
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
