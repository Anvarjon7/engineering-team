package com.zuck.agent;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class AgentRegistry {

    private final Map<String, AgentDefinition> agents;

    public AgentRegistry() {
        this.agents = createAgents();
    }

    public AgentDefinition getAgent(String agentId) {
        AgentDefinition agent = agents.get(agentId);
        if (agent == null) {
            throw new IllegalArgumentException("Unknown agent: " + agentId);
        }
        return agent;
    }

    public Optional<AgentDefinition> findAgent(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }
        String clean = identifier.trim().toLowerCase(Locale.ROOT);
        AgentDefinition exact = agents.get(clean);
        if (exact != null) {
            return Optional.of(exact);
        }

        // Match by agent name (e.g. "Mr. 500", "Sherlock", "Zuck", "Pixel", "Atlas", "Mira", "X", "Ilon")
        for (AgentDefinition agent : agents.values()) {
            if (agent.name().equalsIgnoreCase(clean)) {
                return Optional.of(agent);
            }
        }

        // Match by role name
        for (AgentDefinition agent : agents.values()) {
            if (agent.role().name().equalsIgnoreCase(clean)) {
                return Optional.of(agent);
            }
        }

        // Match alphanumeric stripped (e.g. "mr500" -> "Mr. 500", "teamlead" -> "team_lead")
        String alphaNumeric = clean.replaceAll("[^a-z0-9]", "");
        for (AgentDefinition agent : agents.values()) {
            String agentAlpha = agent.name().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
            String roleAlpha = agent.role().name().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
            if (alphaNumeric.equals(agentAlpha) || alphaNumeric.equals(roleAlpha) || alphaNumeric.equals(agent.id())) {
                return Optional.of(agent);
            }
        }

        // Contains check (e.g. "Mr. 500 (Backend Engineer)", "Sherlock (QA)")
        for (AgentDefinition agent : agents.values()) {
            if (clean.contains(agent.id()) || clean.contains(agent.name().toLowerCase(Locale.ROOT))) {
                return Optional.of(agent);
            }
        }

        return Optional.empty();
    }

    public List<AgentDefinition> getAllAgents() {
        return List.copyOf(agents.values());
    }

    private Map<String, AgentDefinition> createAgents() {
        Map<String, AgentDefinition> definitions = new LinkedHashMap<>();

        register(definitions, new AgentDefinition(
                "zuck", "Zuck", AgentRole.TEAM_LEAD,
                "Coordinates engineering work, decomposes requests, routes discussions, and tracks delivery.",
                Set.of("planning", "task-decomposition", "coordination", "status", "orchestration")));

        register(definitions, new AgentDefinition(
                "backend", "Mr. 500", AgentRole.BACKEND,
                "Owns backend services, APIs, domain logic, databases, performance, and backend tests.",
                Set.of("java", "spring-boot", "postgresql", "rest-api", "backend-tests", "sql")));

        register(definitions, new AgentDefinition(
                "frontend", "Pixel", AgentRole.FRONTEND,
                "Owns web and mobile UI, frontend architecture, UX, API integration, and frontend tests.",
                Set.of("react", "nextjs", "typescript", "flutter", "api-integration", "frontend-tests")));

        register(definitions, new AgentDefinition(
                "qa", "Sherlock", AgentRole.QA,
                "Owns acceptance criteria, test strategy, edge cases, regression checks, and quality assurance.",
                Set.of("test-planning", "api-testing", "edge-cases", "regression", "acceptance")));

        register(definitions, new AgentDefinition(
                "platform", "Atlas", AgentRole.PLATFORM,
                "Owns infrastructure, Docker, CI/CD, deployment pipelines, configuration, and security.",
                Set.of("docker", "ci-cd", "configuration", "security", "operations", "deployment")));

        register(definitions, new AgentDefinition(
                "product", "Mira", AgentRole.PRODUCT,
                "Clarifies requirements, user value, feature roadmap, scope, and user behavior.",
                Set.of("requirements", "acceptance-criteria", "product-design", "user-stories")));

        register(definitions, new AgentDefinition(
                "research", "X", AgentRole.RESEARCH,
                "Researches technical options, third-party libraries, APIs, documentation, and architecture alternatives.",
                Set.of("technical-research", "documentation", "architecture-research", "benchmarks")));

        register(definitions, new AgentDefinition(
                "ilon", "Ilon", AgentRole.REVIEWER,
                "Independently reviews PRs, code quality, architecture standards, tests, and security boundaries.",
                Set.of("code-review", "architecture-review", "quality-gate", "pr-review")));

        return Collections.unmodifiableMap(definitions);
    }

    private void register(Map<String, AgentDefinition> definitions, AgentDefinition definition) {
        definitions.put(definition.id(), definition);
    }
}
