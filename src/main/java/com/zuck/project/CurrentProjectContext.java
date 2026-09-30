package com.zuck.project;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CurrentProjectContext {

    private final ProjectRegistry projectRegistry;
    private volatile String currentProjectId;

    public CurrentProjectContext(
            ProjectRegistry projectRegistry,
            @Value("${project.context.current-project-id:default}") String initialProjectId) {
        this.projectRegistry = projectRegistry;
        if (projectRegistry.hasProject(initialProjectId)) {
            this.currentProjectId = initialProjectId;
        } else if (!projectRegistry.getAllProjects().isEmpty()) {
            this.currentProjectId = projectRegistry.getAllProjects().get(0).id();
        } else {
            this.currentProjectId = initialProjectId;
        }
    }

    public Project getCurrentProject() {
        return projectRegistry.getProject(currentProjectId);
    }

    public String getCurrentProjectId() {
        return currentProjectId;
    }

    public synchronized void setCurrentProject(String projectId) {
        projectRegistry.getProject(projectId); // validates existence
        this.currentProjectId = projectId;
    }

    public List<Project> getAllProjects() {
        return projectRegistry.getAllProjects();
    }
}
