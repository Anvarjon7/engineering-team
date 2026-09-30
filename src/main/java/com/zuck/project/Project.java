package com.zuck.project;

import java.util.List;

public record Project(
        String id,
        String name,
        String description,
        String repository,
        String defaultBranch,
        List<String> technologies) {

    public Project {
        technologies = technologies == null ? List.of() : List.copyOf(technologies);
    }
}
