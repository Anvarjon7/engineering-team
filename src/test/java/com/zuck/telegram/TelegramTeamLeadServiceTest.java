package com.zuck.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zuck.agent.AgentRegistry;
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

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        Project p = new Project(
                "p1", "Project 1", "Desc", "org/p1", "main", List.of("Java"));
        ProjectRegistryProperties props = new ProjectRegistryProperties();
        props.setProjects(Map.of("p1", p));
        ProjectRegistry registry = new ProjectRegistry(props);
        CurrentProjectContext context = new CurrentProjectContext(registry, "p1");
        EngineeringTeam team = new EngineeringTeam(new AgentRegistry());
        coordinator = new TeamCoordinator(team, context);
    }

    @Test
    void handlesMessageWithoutTextSilently() throws Exception {
        TelegramTeamLeadService service = new TelegramTeamLeadService(
                objectMapper,
                coordinator,
                "",
                123L,
                456L,
                true,
                false);

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
                "",
                123L,
                456L,
                true,
                false);

        JsonNode update = objectMapper.readTree("""
                {"update_id": 2, "message": {"chat": {"id": 999}, "from": {"id": 123}, "text": "/whoami"}}
                """);

        assertDoesNotThrow(() -> service.handleUpdate(update));
    }
}
