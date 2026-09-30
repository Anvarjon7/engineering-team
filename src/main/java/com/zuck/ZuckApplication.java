package com.zuck;

import com.zuck.project.ProjectRegistryProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(ProjectRegistryProperties.class)
public class ZuckApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZuckApplication.class, args);
    }
}
