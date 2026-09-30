package com.zuck.llm;

import com.zuck.agent.AgentDefinition;
import com.zuck.project.Project;
import com.zuck.work.WorkItem;

import java.util.ArrayList;
import java.util.List;

public class MockLlmClient implements LlmClient {

    @Override
    public String generateText(String systemPrompt, String userPrompt) {
        return "Mock response for: " + userPrompt;
    }

    @Override
    public String transcribeAudio(byte[] audioBytes, String mimeType) {
        return "Zuck, let's create a leaderboard table which illustrates the weekly performance of each participants in habit-coach project";
    }

    @Override
    public TeamDiscussion generateDiscussion(
            Project project,
            List<AgentDefinition> participants,
            String userRequest,
            WorkItem workItem) {

        List<AgentStatement> statements = new ArrayList<>();

        for (AgentDefinition agent : participants) {
            String msg = switch (agent.role()) {
                case TEAM_LEAD -> "Team, Anwar requested: \"" + userRequest + "\". Let's coordinate the delivery.";
                case PRODUCT -> "I'll define the user stories and ranking calculation for this leaderboard.";
                case BACKEND -> "I'll design the weekly aggregation query and REST endpoints.";
                case FRONTEND -> "I'll build the leaderboard UI view.";
                case QA -> "I'll prepare test scenarios covering edge cases like zero check-ins and tie-breaks.";
                case PLATFORM -> "I'll review CI pipeline and deployment needs.";
                case RESEARCH -> "I'll investigate optimal caching patterns for leaderboard queries.";
                case REVIEWER -> "I'll conduct an independent architectural review of the pull request.";
            };
            statements.add(new AgentStatement(agent.id(), agent.name(), agent.role(), msg));
        }

        return new TeamDiscussion(project, workItem, userRequest, statements);
    }
}
