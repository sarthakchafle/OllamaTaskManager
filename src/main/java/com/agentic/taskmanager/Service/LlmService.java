package com.agentic.taskmanager.Service;

import com.agentic.taskmanager.DTO.LLMResponseDTO;
import com.agentic.taskmanager.DTO.LlmRequestDTO;
import com.agentic.taskmanager.Model.LlmResponse;
import com.agentic.taskmanager.Repository.LLMResponseRepository;
import com.agentic.taskmanager.Repository.SubTaskRepository;
import com.agentic.taskmanager.Repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.MediaType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LlmService {

    private final LLMResponseRepository responseRepo;
    private final TaskRepository taskRepo;
    private final SubTaskRepository subtaskRepo;
    private final RedisTemplate<String, String> redisTemplate;
    private final LocalLLMClient llmClient;

    public LlmService(LLMResponseRepository responseRepo,
                      TaskRepository taskRepo,
                      SubTaskRepository subtaskRepo,
                      RedisTemplate<String, String> redisTemplate,
                      LocalLLMClient llmClient) {
        this.responseRepo = responseRepo;
        this.taskRepo = taskRepo;
        this.subtaskRepo = subtaskRepo;
        this.redisTemplate = redisTemplate;
        this.llmClient = llmClient;
    }

    public LLMResponseDTO handleUserPrompt(LlmRequestDTO dto) {
        String key = "llm:" + dto.getPrompt();
        String cached = redisTemplate.opsForValue().get(key);

        if (cached != null) {
            return new LLMResponseDTO(dto.getPrompt(), cached);
        }

        String response = llmClient.askLLM(dto.getPrompt());

        LlmResponse entity = new LlmResponse();
        entity.setPrompt(dto.getPrompt());
        entity.setResponse(response);

        if (dto.getTaskId() != null) {
            taskRepo.findById(dto.getTaskId()).ifPresent(entity::setTask);
        }
        if (dto.getSubtaskId() != null) {
            subtaskRepo.findById(dto.getSubtaskId()).ifPresent(entity::setSubTask);
        }

        responseRepo.save(entity);
        redisTemplate.opsForValue().set(key, response, Duration.ofSeconds(30));

        return new LLMResponseDTO(dto.getPrompt(), response);
    }

    public List<LlmResponse> getHistoryForTask(Long taskId) {
        return responseRepo.findByTaskId(taskId);
    }

    public List<LlmResponse> getHistoryForSubtask(Long subtaskId) {
        return responseRepo.findBySubTaskId(subtaskId);
    }
}
