package com.zuck.team;

import com.zuck.agent.AgentDefinition;
import com.zuck.agent.AgentRegistry;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EngineeringTeam {

    private final AgentRegistry agentRegistry;

    public EngineeringTeam(AgentRegistry agentRegistry) {
        this.agentRegistry = agentRegistry;
    }

    public List<AgentDefinition> getMembers() {
        return agentRegistry.getAllAgents();
    }

    public AgentDefinition getMember(String agentId) {
        return agentRegistry.getAgent(agentId);
    }
}
