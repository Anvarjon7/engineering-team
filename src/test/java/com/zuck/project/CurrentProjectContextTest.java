package com.zuck.project;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CurrentProjectContextTest {

    @Test
    void allowsSwitchingActiveProjectContext() {
        Project p1 = new Project(
                "p1", "Project One", "Description 1", "org/p1", "main", List.of("Java"));
        Project p2 = new Project(
                "p2", "Project Two", "Description 2", "org/p2", "main", List.of("TypeScript"));

        ProjectRegistryProperties props = new ProjectRegistryProperties();
        props.setProjects(Map.of("p1", p1, "p2", p2));
        ProjectRegistry registry = new ProjectRegistry(props);

        CurrentProjectContext context = new CurrentProjectContext(registry, "p1");
        assertEquals("p1", context.getCurrentProjectId());
        assertEquals("Project One", context.getCurrentProject().name());

        context.setCurrentProject("p2");
        assertEquals("p2", context.getCurrentProjectId());
        assertEquals("Project Two", context.getCurrentProject().name());

        assertThrows(IllegalArgumentException.class, () -> context.setCurrentProject("p-invalid"));
    }
}
