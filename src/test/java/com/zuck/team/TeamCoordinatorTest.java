package com.zuck.team;

import com.zuck.agent.AgentRegistry;
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
        coordinator = new TeamCoordinator(team, context);
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
        assertFalse(participantIds.contains("frontend"));
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
    void coordinatesDevOpsPlatformTask() {
        TeamCoordinationResult result = coordinator.coordinate(
                "Configure Docker compose deployment pipeline with CI security checks");

        List<String> participantIds = result.participants().stream().map(a -> a.id()).toList();
        assertTrue(participantIds.contains("zuck"));
        assertTrue(participantIds.contains("platform"));
    }

    @Test
    void supportsSwitchingActiveProjectContext() {
        assertEquals("habit-coach-agent", coordinator.getCurrentProject().id());

        coordinator.switchProject("ai-academy");
        assertEquals("ai-academy", coordinator.getCurrentProject().id());

        TeamCoordinationResult result = coordinator.coordinate("Implement Next.js frontend UI");
        assertEquals("ai-academy", result.project().id());
    }

    @Test
    void rejectsBlankRequest() {
        assertThrows(IllegalArgumentException.class, () -> coordinator.coordinate("   "));
    }
}
