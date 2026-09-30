package com.zuck.llm;

import com.zuck.agent.AgentRole;
import com.zuck.project.Project;
import com.zuck.work.WorkItem;

import java.util.List;

public record TeamDiscussion(
        Project project,
        WorkItem workItem,
        String summary,
        List<AgentStatement> statements) {

    public TeamDiscussion {
        statements = statements == null ? List.of() : List.copyOf(statements);
    }

    public String toFormattedTelegramMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 Team Lead Meeting [Project: ").append(project.name()).append("]\n");
        sb.append("Status: ").append(workItem.status()).append("\n");
        if (summary != null && !summary.isBlank()) {
            sb.append("Topic: ").append(summary.trim()).append("\n");
        }
        sb.append("─────────────────────────\n\n");

        for (AgentStatement stmt : statements) {
            String emoji = getRoleEmoji(stmt.role());
            sb.append(emoji).append(" ").append(stmt.agentName())
              .append(" (").append(formatRoleName(stmt.role())).append("):\n")
              .append("\"").append(stmt.message().trim()).append("\"\n\n");
        }

        sb.append("─────────────────────────\n");
        sb.append("💡 WorkItem logged. Ready for review and execution planning.");
        return sb.toString().trim();
    }

    private String getRoleEmoji(AgentRole role) {
        return switch (role) {
            case TEAM_LEAD -> "🤖";
            case BACKEND -> "🧑‍💻";
            case FRONTEND -> "🎨";
            case QA -> "🔍";
            case PLATFORM -> "🛠️";
            case PRODUCT -> "📋";
            case RESEARCH -> "🔬";
            case REVIEWER -> "⚖️";
        };
    }

    private String formatRoleName(AgentRole role) {
        return switch (role) {
            case TEAM_LEAD -> "Team Lead";
            case BACKEND -> "Backend Engineer";
            case FRONTEND -> "Frontend Engineer";
            case QA -> "QA Engineer";
            case PLATFORM -> "DevOps & Platform";
            case PRODUCT -> "Product Manager";
            case RESEARCH -> "Technical Research";
            case REVIEWER -> "Independent Reviewer";
        };
    }
}
