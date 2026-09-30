package com.zuck.agent;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
                "backend", "Mr.500", AgentRole.BACKEND,
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
