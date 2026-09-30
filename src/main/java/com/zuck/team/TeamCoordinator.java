package com.zuck.team;

import com.zuck.agent.AgentDefinition;
import com.zuck.llm.LlmClient;
import com.zuck.llm.TeamDiscussion;
import com.zuck.project.CurrentProjectContext;
import com.zuck.project.Project;
import com.zuck.work.WorkItem;
import com.zuck.work.WorkItemStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class TeamCoordinator {

    private final EngineeringTeam engineeringTeam;
    private final CurrentProjectContext projectContext;
    private final LlmClient llmClient;

    public TeamCoordinator(
            EngineeringTeam engineeringTeam,
            CurrentProjectContext projectContext,
            LlmClient llmClient) {
        this.engineeringTeam = engineeringTeam;
        this.projectContext = projectContext;
        this.llmClient = llmClient;
    }

    public Project getCurrentProject() {
        return projectContext.getCurrentProject();
    }

    public void switchProject(String projectId) {
        projectContext.setCurrentProject(projectId);
    }

    public List<Project> listProjects() {
        return projectContext.getAllProjects();
    }

    public List<AgentDefinition> getTeam() {
        return engineeringTeam.getMembers();
    }

    /**
     * Standard coordination: resolves participants and creates work item.
     */
    public TeamCoordinationResult coordinate(String request) {
        if (request == null || request.isBlank()) {
            throw new IllegalArgumentException("request must not be blank");
        }

        detectAndSwitchProjectIfMentioned(request);

        Project project = getCurrentProject();
        Set<String> roles = determineRelevantRoles(request);
        List<AgentDefinition> participants = resolveParticipants(roles);

        WorkItem workItem = new WorkItem(
                project.id(),
                request.trim(),
                WorkItemStatus.DISCUSSION,
                participants.stream().map(AgentDefinition::id).toList());

        return new TeamCoordinationResult(project, participants, workItem);
    }

    /**
     * Convenes the full virtual engineering team meeting:
     * Decomposes the task, assigns specialists, and runs multi-agent dialogue.
     */
    public TeamDiscussion coordinateAndDiscuss(String request) {
        TeamCoordinationResult coord = coordinate(request);
        return llmClient.generateDiscussion(
                coord.project(),
                coord.participants(),
                request.trim(),
                coord.workItem());
    }

    private void detectAndSwitchProjectIfMentioned(String request) {
        String lower = request.toLowerCase(Locale.ROOT);
        for (Project p : projectContext.getAllProjects()) {
            String pId = p.id().toLowerCase(Locale.ROOT);
            String pName = p.name().toLowerCase(Locale.ROOT);
            String normalizedId = pId.replace("-", " ");

            if (lower.contains(pId) || lower.contains(pName) || lower.contains(normalizedId)) {
                projectContext.setCurrentProject(p.id());
                break;
            }
        }
    }

    private List<AgentDefinition> resolveParticipants(Set<String> roles) {
        List<AgentDefinition> participants = new ArrayList<>();
        for (AgentDefinition agent : engineeringTeam.getMembers()) {
            if (roles.contains(agent.role().name())) {
                participants.add(agent);
            }
        }
        return participants;
    }

    private Set<String> determineRelevantRoles(String request) {
        String text = request.toLowerCase(Locale.ROOT);
        Set<String> roles = new LinkedHashSet<>();

        // Team Lead (Zuck) always coordinates
        roles.add("TEAM_LEAD");

        if (containsAny(text, "ui", "frontend", "web", "mobile", "screen", "react", "nextjs", "flutter", "css", "layout", "view", "table", "dashboard")) {
            roles.add("FRONTEND");
        }

        if (containsAny(text, "api", "backend", "java", "spring", "database", "postgres", "sql", "endpoint", "rest", "service", "jpa", "leaderboard", "data", "query", "aggregation", "streak", "counter", "check-in", "check in", "entity", "logic", "calculate", "store", "save", "fetch", "model", "crud", "controller", "repository", "migration", "table", "schema", "record", "track")) {
            roles.add("BACKEND");
        }

        if (containsAny(text, "test", "bug", "qa", "regression", "acceptance", "verify", "criteria", "edge case", "validate", "check", "coverage", "case", "scenario")) {
            roles.add("QA");
        }

        if (containsAny(text, "docker", "deploy", "deployment", "ci", "cd", "infrastructure", "security", "production", "pipeline", "k8s")) {
            roles.add("PLATFORM");
        }

        if (containsAny(text, "feature", "user", "requirement", "behavior", "product", "leaderboard", "scope", "story", "points", "ranking", "streak", "habit", "goal", "coach", "rule", "metric", "flow", "journey")) {
            roles.add("PRODUCT");
        }

        if (containsAny(text, "research", "compare", "library", "documentation", "alternative", "investigate", "explore")) {
            roles.add("RESEARCH");
        }

        // If no specialists were matched, default to core team (Backend, QA, Product)
        if (roles.size() == 1) {
            roles.add("BACKEND");
            roles.add("QA");
            roles.add("PRODUCT");
        }

        return roles;
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }
}
