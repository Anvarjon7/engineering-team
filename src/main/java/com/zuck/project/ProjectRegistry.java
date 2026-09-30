package com.zuck.project;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ProjectRegistry {

    private final Map<String, Project> projects;

    public ProjectRegistry(ProjectRegistryProperties properties) {
        this.projects = Map.copyOf(properties.getProjects());
    }

    public Project getProject(String projectId) {
        Project project = projects.get(projectId);
        if (project == null) {
            throw new IllegalArgumentException("Unknown project: " + projectId);
        }
        return project;
    }

    public boolean hasProject(String projectId) {
        return projects.containsKey(projectId);
    }

    public List<Project> getAllProjects() {
        return List.copyOf(projects.values());
    }
}
