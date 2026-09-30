package com.zuck.llm;

import com.zuck.agent.AgentRole;

public record AgentStatement(
        String agentId,
        String agentName,
        AgentRole role,
        String message) {
}
