package com.zuck.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AgentRegistryTest {

    private AgentRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new AgentRegistry();
    }

    @Test
    void exposesCompleteProjectIndependentTeam() {
        List<String> ids = registry.getAllAgents().stream()
                .map(AgentDefinition::id)
                .toList();

        assertEquals(
                List.of("zuck", "backend", "frontend", "qa", "platform", "product", "research", "ilon"),
                ids);
    }

    @Test
    void exposesAllEightAgentPersonasCorrectly() {
        AgentDefinition zuck = registry.getAgent("zuck");
        assertEquals("Zuck", zuck.name());
        assertEquals(AgentRole.TEAM_LEAD, zuck.role());

        AgentDefinition backend = registry.getAgent("backend");
        assertEquals("Mr.500", backend.name());
        assertEquals(AgentRole.BACKEND, backend.role());
        assertTrue(backend.capabilities().contains("java"));
        assertTrue(backend.capabilities().contains("postgresql"));

        AgentDefinition frontend = registry.getAgent("frontend");
        assertEquals("Pixel", frontend.name());
        assertEquals(AgentRole.FRONTEND, frontend.role());
        assertTrue(frontend.capabilities().contains("react"));
        assertTrue(frontend.capabilities().contains("flutter"));

        AgentDefinition qa = registry.getAgent("qa");
        assertEquals("Sherlock", qa.name());
        assertEquals(AgentRole.QA, qa.role());

        AgentDefinition platform = registry.getAgent("platform");
        assertEquals("Atlas", platform.name());
        assertEquals(AgentRole.PLATFORM, platform.role());

        AgentDefinition product = registry.getAgent("product");
        assertEquals("Mira", product.name());
        assertEquals(AgentRole.PRODUCT, product.role());

        AgentDefinition research = registry.getAgent("research");
        assertEquals("X", research.name());
        assertEquals(AgentRole.RESEARCH, research.role());

        AgentDefinition ilon = registry.getAgent("ilon");
        assertEquals("Ilon", ilon.name());
        assertEquals(AgentRole.REVIEWER, ilon.role());
    }

    @Test
    void rejectsUnknownAgent() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registry.getAgent("unknown-agent"));

        assertEquals("Unknown agent: unknown-agent", exception.getMessage());
    }
}
