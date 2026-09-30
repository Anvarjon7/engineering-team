package com.zuck.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zuck.agent.AgentRegistry;
import com.zuck.llm.MockLlmClient;
import com.zuck.project.CurrentProjectContext;
import com.zuck.project.Project;
import com.zuck.project.ProjectRegistry;
import com.zuck.project.ProjectRegistryProperties;
import com.zuck.team.EngineeringTeam;
import com.zuck.team.TeamCoordinator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class TelegramTeamLeadServiceTest {

    private ObjectMapper objectMapper;
    private TeamCoordinator coordinator;
    private MockLlmClient mockLlmClient;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockLlmClient = new MockLlmClient();

        Project p = new Project(
                "habit-coach-agent", "Habit Coach Agent", "Desc", "org/p1", "main", List.of("Java"));
        ProjectRegistryProperties props = new ProjectRegistryProperties();
        props.setProjects(Map.of(p.id(), p));
        ProjectRegistry registry = new ProjectRegistry(props);
        CurrentProjectContext context = new CurrentProjectContext(registry, p.id());
        EngineeringTeam team = new EngineeringTeam(new AgentRegistry());
        coordinator = new TeamCoordinator(team, context, mockLlmClient);
    }

    @Test
    void handlesMessageWithoutTextSilently() throws Exception {
        TelegramTeamLeadService service = new TelegramTeamLeadService(
                objectMapper,
                coordinator,
                mockLlmClient,
                "",
                123L,
                456L,
                true,
                true);

        JsonNode update = objectMapper.readTree("""
                {"update_id": 1, "message": {"chat": {"id": 456}, "from": {"id": 123}}}
                """);

        assertDoesNotThrow(() -> service.handleUpdate(update));
    }

    @Test
    void ignoresMessagesFromOtherChatsWhenFiltered() throws Exception {
        TelegramTeamLeadService service = new TelegramTeamLeadService(
                objectMapper,
                coordinator,
                mockLlmClient,
                "",
                123L,
                456L,
                true,
                true);

        JsonNode update = objectMapper.readTree("""
                {"update_id": 2, "message": {"chat": {"id": 999}, "from": {"id": 123}, "text": "/whoami"}}
                """);

        assertDoesNotThrow(() -> service.handleUpdate(update));
    }

    @Test
    void handlesNaturalLanguageTaskPromptWithoutError() throws Exception {
        TelegramTeamLeadService service = new TelegramTeamLeadService(
                objectMapper,
                coordinator,
                mockLlmClient,
                "",
                123L,
                456L,
                true,
                true);

        JsonNode update = objectMapper.readTree("""
                {
                  "update_id": 3,
                  "message": {
                    "chat": {"id": 456},
                    "from": {"id": 123},
                    "text": "zuck let's create a leaderboard table which illustrates the weekly performance of each participants in habit-coach project"
                  }
                }
                """);

        assertDoesNotThrow(() -> service.handleUpdate(update));
    }
}
