package com.zuck.agent;

import java.util.Set;

public record AgentDefinition(
        String id,
        String name,
        AgentRole role,
        String description,
        Set<String> capabilities) {
}
