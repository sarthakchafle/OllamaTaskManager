package com.agentic.taskmanager.Feign;

import com.agentic.taskmanager.Model.TaskRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "google-calendar-service", url = "http://host.docker.internal:9000")
public interface TaskProcessorClient {
    @PostMapping("/api/orchestrator/process-prompt")
    String processTask(@RequestBody TaskRequest request);
}