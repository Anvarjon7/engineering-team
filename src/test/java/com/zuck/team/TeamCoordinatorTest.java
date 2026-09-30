package com.zuck.team;

import com.zuck.agent.AgentRegistry;
import com.zuck.llm.MockLlmClient;
import com.zuck.llm.TeamDiscussion;
import com.zuck.project.CurrentProjectContext;
import com.zuck.project.Project;
import com.zuck.project.ProjectRegistry;
import com.zuck.project.ProjectRegistryProperties;
import com.zuck.work.WorkItemStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TeamCoordinatorTest {

    private TeamCoordinator coordinator;
    private CurrentProjectContext context;
    private Project project1;
    private Project project2;

    @BeforeEach
    void setUp() {
        project1 = new Project(
                "habit-coach-agent", "Habit Coach Agent", "Habit coaching platform",
                "Anvarjon7/habit-coach-agent", "main",
                List.of("Java", "Spring Boot", "PostgreSQL"));

        project2 = new Project(
                "ai-academy", "AI Academy", "Education platform",
                "Anvarjon7/ai-academy", "main",
                List.of("TypeScript", "Next.js", "Python"));

        ProjectRegistryProperties properties = new ProjectRegistryProperties();
        properties.setProjects(Map.of(
                project1.id(), project1,
                project2.id(), project2));

        ProjectRegistry projectRegistry = new ProjectRegistry(properties);
        context = new CurrentProjectContext(projectRegistry, project1.id());
        EngineeringTeam team = new EngineeringTeam(new AgentRegistry());
        coordinator = new TeamCoordinator(team, context, new MockLlmClient());
    }

    @Test
    void coordinatesBackendAndQaTask() {
        TeamCoordinationResult result = coordinator.coordinate(
                "Add a Java backend REST API endpoint and acceptance tests for weekly leaderboard");

        assertEquals(project1.id(), result.project().id());
        assertEquals(WorkItemStatus.DISCUSSION, result.workItem().status());

        List<String> participantIds = result.participants().stream().map(a -> a.id()).toList();
        assertTrue(participantIds.contains("zuck"));
        assertTrue(participantIds.contains("backend"));
        assertTrue(participantIds.contains("qa"));
        assertTrue(participantIds.contains("product"));
        assertFalse(participantIds.contains("platform"));
    }

    @Test
    void coordinatesFrontendAndProductTask() {
        TeamCoordinationResult result = coordinator.coordinate(
                "Create a React web dashboard screen for user habit streaks");

        List<String> participantIds = result.participants().stream().map(a -> a.id()).toList();
        assertTrue(participantIds.contains("zuck"));
        assertTrue(participantIds.contains("frontend"));
        assertTrue(participantIds.contains("product"));
        assertFalse(participantIds.contains("platform"));
    }

    @Test
    void automaticallyDetectsProjectMentionedInTask() {
        assertEquals("habit-coach-agent", coordinator.getCurrentProject().id());

        // Mentions ai-academy in request
        TeamCoordinationResult result = coordinator.coordinate(
                "Zuck, let's create a leaderboard for ai-academy project with tests");

        assertEquals("ai-academy", coordinator.getCurrentProject().id());
        assertEquals("ai-academy", result.project().id());
    }

    @Test
    void coordinatesAndGeneratesMultiAgentDiscussion() {
        TeamDiscussion discussion = coordinator.coordinateAndDiscuss(
                "Zuck, let's create a leaderboard table and test cases illustrating the weekly performance of each participants in habit-coach project");

        assertEquals("habit-coach-agent", discussion.project().id());
        assertFalse(discussion.statements().isEmpty());

        List<String> speakingAgents = discussion.statements().stream()
                .map(s -> s.agentId())
                .toList();

        assertTrue(speakingAgents.contains("zuck"));
        assertTrue(speakingAgents.contains("backend"));
        assertTrue(speakingAgents.contains("qa"));

        String formatted = discussion.toFormattedTelegramMessage();
        assertTrue(formatted.contains("Zuck"));
        assertTrue(formatted.contains("Mr.500"));
        assertTrue(formatted.contains("Sherlock"));
    }

    @Test
    void rejectsBlankRequest() {
        assertThrows(IllegalArgumentException.class, () -> coordinator.coordinate("   "));
    }
}
