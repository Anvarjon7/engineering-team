package com.zuck.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zuck.agent.AgentDefinition;
import com.zuck.agent.AgentRegistry;
import com.zuck.project.Project;
import com.zuck.work.WorkItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class GeminiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiLlmClient.class);

    private final ObjectMapper objectMapper;
    private final AgentRegistry agentRegistry;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;

    public GeminiLlmClient(
            ObjectMapper objectMapper,
            AgentRegistry agentRegistry,
            @Value("${gemini.api-key:${GEMINI_API_KEY:}}") String apiKey,
            @Value("${gemini.model:gemini-2.5-flash}") String model) {
        this.objectMapper = objectMapper;
        this.agentRegistry = agentRegistry;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = model != null && !model.isBlank() ? model.trim() : "gemini-2.5-flash";
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    @Override
    public String generateText(String systemPrompt, String userPrompt) {
        if (!isConfigured()) {
            return "LLM API Key not configured. Using default team response.";
        }

        try {
            ObjectNode root = objectMapper.createObjectNode();

            if (systemPrompt != null && !systemPrompt.isBlank()) {
                ObjectNode sysInstruction = root.putObject("system_instruction");
                ArrayNode sysParts = sysInstruction.putArray("parts");
                sysParts.addObject().put("text", systemPrompt);
            }

            ArrayNode contents = root.putArray("contents");
            ObjectNode userContent = contents.addObject();
            userContent.put("role", "user");
            ArrayNode userParts = userContent.putArray("parts");
            userParts.addObject().put("text", userPrompt);

            String responseBody = sendGeminiRequest(root);
            return extractTextFromResponse(responseBody);
        } catch (Exception e) {
            log.error("Failed to generate text from Gemini API", e);
            return "Gemini API call failed: " + e.getMessage();
        }
    }

    @Override
    public String transcribeAudio(byte[] audioBytes, String mimeType) {
        if (!isConfigured()) {
            return "Transcription unavailable: GEMINI_API_KEY not configured.";
        }

        try {
            String base64Data = Base64.getEncoder().encodeToString(audioBytes);
            String effectiveMimeType = (mimeType == null || mimeType.isBlank()) ? "audio/ogg" : mimeType;

            ObjectNode root = objectMapper.createObjectNode();
            ArrayNode contents = root.putArray("contents");
            ObjectNode userContent = contents.addObject();
            userContent.put("role", "user");
            ArrayNode parts = userContent.putArray("parts");

            ObjectNode inlineData = parts.addObject().putObject("inline_data");
            inlineData.put("mime_type", effectiveMimeType);
            inlineData.put("data", base64Data);

            parts.addObject().put("text",
                    "Transcribe the spoken words in this audio recording accurately. "
                    + "Return ONLY the transcription text without any additional comments, preamble, or markdown formatting.");

            String responseBody = sendGeminiRequest(root);
            String transcribed = extractTextFromResponse(responseBody);
            return transcribed.trim();
        } catch (Exception e) {
            log.error("Failed to transcribe audio with Gemini", e);
            return "Audio transcription failed: " + e.getMessage();
        }
    }

    @Override
    public TeamDiscussion generateDiscussion(
            Project project,
            List<AgentDefinition> participants,
            String userRequest,
            WorkItem workItem) {

        if (!isConfigured()) {
            return fallbackDiscussion(project, participants, userRequest, workItem);
        }

        try {
            Map<String, AgentDefinition> participantMap = participants.stream()
                    .collect(Collectors.toMap(AgentDefinition::id, a -> a));

            String systemPrompt = buildDiscussionSystemPrompt(project, participants);
            String prompt = String.format("""
                    The project owner / engineering manager Anwar sent this request for project '%s':
                    "%s"

                    Conduct an authentic, high-quality, professional engineering team discussion among the participating agents.
                    Each participating agent must speak in character with their specific expertise.
                    Return your response strictly as valid JSON matching this schema:
                    {
                      "summary": "Concise 3-7 word summary of the task topic",
                      "statements": [
                        {
                          "agentId": "<exact agent ID from the participants list, e.g. 'zuck', 'backend', 'frontend', 'qa', 'platform', 'product', 'research', 'ilon'>",
                          "message": "<what this engineer says>"
                        }
                      ]
                    }
                    CRITICAL INSTRUCTION:
                    - In 'statements', the 'agentId' MUST correspond to the agent speaking (e.g. 'product' for Mira, 'backend' for Mr. 500, 'qa' for Sherlock).
                    - DO NOT assign all statements to 'zuck'.
                    """, project.name(), userRequest);

            ObjectNode root = objectMapper.createObjectNode();

            ObjectNode sysInstruction = root.putObject("system_instruction");
            ArrayNode sysParts = sysInstruction.putArray("parts");
            sysParts.addObject().put("text", systemPrompt);

            ArrayNode contents = root.putArray("contents");
            ObjectNode userContent = contents.addObject();
            userContent.put("role", "user");
            ArrayNode userParts = userContent.putArray("parts");
            userParts.addObject().put("text", prompt);

            ObjectNode genConfig = root.putObject("generationConfig");
            genConfig.put("responseMimeType", "application/json");

            String responseBody = sendGeminiRequest(root);
            String responseText = extractTextFromResponse(responseBody);
            return parseDiscussionJson(responseText, project, workItem, participantMap);
        } catch (Exception e) {
            log.error("Failed to generate dynamic team discussion via LLM, using fallback", e);
            return fallbackDiscussion(project, participants, userRequest, workItem);
        }
    }

    private String buildDiscussionSystemPrompt(Project project, List<AgentDefinition> participants) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are the AI Software Engineering Team platform named 'Zuck'.\n");
        sb.append("You simulate a real, elite virtual software engineering team collaborating on client software projects.\n\n");

        sb.append("CURRENT PROJECT CONTEXT:\n");
        sb.append("• Project ID: ").append(project.id()).append("\n");
        sb.append("• Name: ").append(project.name()).append("\n");
        sb.append("• Description: ").append(project.description()).append("\n");
        sb.append("• Repository: ").append(project.repository()).append(" (branch: ").append(project.defaultBranch()).append(")\n");
        sb.append("• Technologies: ").append(String.join(", ", project.technologies())).append("\n\n");

        sb.append("PARTICIPATING AGENTS IN THIS DISCUSSION:\n");
        for (AgentDefinition agent : participants) {
            sb.append("• ID: '").append(agent.id()).append("', Name: ").append(agent.name())
              .append(" (").append(agent.role().name()).append(")\n")
              .append("  Expertise: ").append(agent.description()).append("\n")
              .append("  Key capabilities: ").append(String.join(", ", agent.capabilities())).append("\n");
        }

        sb.append("\nGUIDELINES FOR THE DISCUSSION:\n");
        sb.append("1. Zuck (Team Lead) speaks first: acknowledges Anwar's request, summarizes the goal, and directs questions to specialists.\n");
        sb.append("2. Participating specialists (e.g. Mr. 500 for backend, Pixel for frontend, Mira for product, Sherlock for QA) answer with concrete, technical proposals (mentioning real APIs, schemas, edge cases, user stories, or architectures matching the project's tech stack).\n");
        sb.append("3. Zuck wraps up the discussion with next steps.\n");
        sb.append("4. Tone: Collaborative, practical, intelligent, and focused on quality.\n");
        return sb.toString();
    }

    private TeamDiscussion parseDiscussionJson(
            String rawJson,
            Project project,
            WorkItem workItem,
            Map<String, AgentDefinition> participantMap) throws Exception {

        String cleaned = extractJson(rawJson);
        JsonNode root = objectMapper.readTree(cleaned);
        String summary = root.path("summary").asText(workItem.description());
        List<AgentStatement> statements = new ArrayList<>();

        for (JsonNode stmtNode : root.path("statements")) {
            String agentIdStr = stmtNode.has("agentId") ? stmtNode.path("agentId").asText()
                    : stmtNode.has("agent_id") ? stmtNode.path("agent_id").asText()
                    : stmtNode.has("speaker") ? stmtNode.path("speaker").asText()
                    : stmtNode.has("name") ? stmtNode.path("name").asText()
                    : stmtNode.has("role") ? stmtNode.path("role").asText()
                    : "";
            String message = stmtNode.has("message") ? stmtNode.path("message").asText()
                    : stmtNode.has("text") ? stmtNode.path("text").asText()
                    : stmtNode.has("statement") ? stmtNode.path("statement").asText()
                    : "";

            if (message.isBlank()) {
                continue;
            }

            AgentDefinition agent = resolveAgent(agentIdStr, participantMap);
            statements.add(new AgentStatement(agent.id(), agent.name(), agent.role(), message));
        }

        if (statements.isEmpty()) {
            return fallbackDiscussion(project, List.copyOf(participantMap.values()), workItem.description(), workItem);
        }

        return new TeamDiscussion(project, workItem, summary, statements);
    }

    private String extractJson(String text) {
        if (text == null) {
            return "{}";
        }
        String cleaned = text.trim();
        int codeBlockStart = cleaned.indexOf("```json");
        if (codeBlockStart != -1) {
            int contentStart = codeBlockStart + 7;
            int codeBlockEnd = cleaned.indexOf("```", contentStart);
            if (codeBlockEnd != -1) {
                return cleaned.substring(contentStart, codeBlockEnd).trim();
            }
        }
        codeBlockStart = cleaned.indexOf("```");
        if (codeBlockStart != -1) {
            int contentStart = codeBlockStart + 3;
            int codeBlockEnd = cleaned.indexOf("```", contentStart);
            if (codeBlockEnd != -1) {
                return cleaned.substring(contentStart, codeBlockEnd).trim();
            }
        }
        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return cleaned.substring(firstBrace, lastBrace + 1).trim();
        }
        return cleaned;
    }

    private AgentDefinition resolveAgent(String identifier, Map<String, AgentDefinition> participantMap) {
        if (identifier != null && !identifier.isBlank()) {
            String clean = identifier.trim().toLowerCase(Locale.ROOT);
            AgentDefinition fromParticipants = participantMap.get(clean);
            if (fromParticipants != null) {
                return fromParticipants;
            }

            for (AgentDefinition p : participantMap.values()) {
                if (p.name().equalsIgnoreCase(clean) || p.role().name().equalsIgnoreCase(clean)) {
                    return p;
                }
            }

            Optional<AgentDefinition> found = agentRegistry.findAgent(identifier);
            if (found.isPresent()) {
                return found.get();
            }
        }

        return participantMap.getOrDefault("zuck", agentRegistry.getAgent("zuck"));
    }

    private TeamDiscussion fallbackDiscussion(
            Project project,
            List<AgentDefinition> participants,
            String userRequest,
            WorkItem workItem) {

        List<AgentStatement> statements = new ArrayList<>();

        for (AgentDefinition agent : participants) {
            String statement = switch (agent.role()) {
                case TEAM_LEAD -> "Team, Anwar requested: \"" + userRequest + "\". Let's review requirements and implementation architecture for " + project.name() + ".";
                case PRODUCT -> "From product standpoint, we should define clear user stories and acceptance criteria before implementation.";
                case BACKEND -> "Backend is ready. I'll design the endpoints, domain logic, and tests matching our " + String.join("/", project.technologies()) + " stack.";
                case FRONTEND -> "I'll design the user interface components and state management.";
                case QA -> "I will define edge cases, validation rules, and regression tests for these changes.";
                case PLATFORM -> "I'll ensure containerization, CI pipelines, and environment variables are properly configured.";
                case RESEARCH -> "I'll research the best practices and library options for this feature.";
                case REVIEWER -> "I'll review the pull request for quality, security, and architectural integrity once submitted.";
            };
            statements.add(new AgentStatement(agent.id(), agent.name(), agent.role(), statement));
        }

        return new TeamDiscussion(project, workItem, userRequest, statements);
    }

    private String sendGeminiRequest(ObjectNode body) throws Exception {
        String url = String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                model, apiKey);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new RuntimeException("Gemini API error (HTTP " + response.statusCode() + "): " + response.body());
        }

        return response.body();
    }

    private String extractTextFromResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode candidates = root.path("candidates");
        if (candidates.isArray() && !candidates.isEmpty()) {
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (parts.isArray() && !parts.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode part : parts) {
                    if (part.has("text")) {
                        sb.append(part.path("text").asText());
                    }
                }
                return sb.toString();
            }
        }
        return "";
    }
}
