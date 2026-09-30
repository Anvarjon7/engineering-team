package com.zuck.llm;

import com.zuck.agent.AgentDefinition;
import com.zuck.project.Project;
import com.zuck.work.WorkItem;

import java.util.List;

public interface LlmClient {

    /**
     * Generates a plain text response from the model.
     */
    String generateText(String systemPrompt, String userPrompt);

    /**
     * Transcribes an audio recording (e.g. Telegram voice message) into text.
     */
    String transcribeAudio(byte[] audioBytes, String mimeType);

    /**
     * Generates a realistic, domain-specific multi-agent discussion among the participating agents.
     */
    TeamDiscussion generateDiscussion(
            Project project,
            List<AgentDefinition> participants,
            String userRequest,
            WorkItem workItem);
}
