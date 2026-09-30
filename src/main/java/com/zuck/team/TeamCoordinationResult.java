package com.zuck.team;

import com.zuck.agent.AgentDefinition;
import com.zuck.project.Project;
import com.zuck.work.WorkItem;

import java.util.List;

public record TeamCoordinationResult(
        Project project,
        List<AgentDefinition> participants,
        WorkItem workItem) {

    public TeamCoordinationResult {
        participants = participants == null ? List.of() : List.copyOf(participants);
    }
}
