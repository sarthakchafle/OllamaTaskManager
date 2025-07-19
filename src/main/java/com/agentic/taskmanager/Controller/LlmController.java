package com.agentic.taskmanager.Controller;

import com.agentic.taskmanager.DTO.LLMResponseDTO;
import com.agentic.taskmanager.DTO.LlmRequestDTO;
import com.agentic.taskmanager.Model.LlmResponse;
import com.agentic.taskmanager.Service.LlmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class LlmController {

    @Autowired
    private final LlmService service;

    public LlmController(LlmService service) {
        this.service = service;
    }

    @PostMapping("/ask")
    public ResponseEntity<LLMResponseDTO> ask(@RequestBody LlmRequestDTO dto) {
        return ResponseEntity.ok(service.handleUserPrompt(dto));
    }

    @GetMapping("/history/task/{taskId}")
    public List<LlmResponse> getTaskHistory(@PathVariable Long taskId) {
        return service.getHistoryForTask(taskId);
    }

    @GetMapping("/history/subtask/{subtaskId}")
    public List<LlmResponse> getSubtaskHistory(@PathVariable Long subtaskId) {
        return service.getHistoryForSubtask(subtaskId);
    }
}

