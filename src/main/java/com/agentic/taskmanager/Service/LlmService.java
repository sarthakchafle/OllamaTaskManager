package com.agentic.taskmanager.Service;

import com.agentic.taskmanager.DTO.LLMResponseDTO;
import com.agentic.taskmanager.DTO.LlmRequestDTO;
import com.agentic.taskmanager.Model.LlmResponse;
import com.agentic.taskmanager.Repository.LLMResponseRepository;
import com.agentic.taskmanager.Repository.SubTaskRepository;
import com.agentic.taskmanager.Repository.TaskRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    public LlmService(LLMResponseRepository responseRepo,
                      TaskRepository taskRepo,
                      SubTaskRepository subtaskRepo,
                      RedisTemplate<String, String> redisTemplate,
                      LocalLLMClient llmClient,ObjectMapper objectMapper) {
        this.responseRepo = responseRepo;
        this.taskRepo = taskRepo;
        this.subtaskRepo = subtaskRepo;
        this.redisTemplate = redisTemplate;
        this.llmClient = llmClient;
        this.objectMapper=objectMapper;
    }

    public LLMResponseDTO handleUserPrompt(LlmRequestDTO dto) {
        String key = "llm:" + dto.getPrompt();
        String cachedFullOllamaJson = redisTemplate.opsForValue().get(key); // Get full JSON string from cache
        String extractedResponseText; // This will hold ONLY the plain text response

        // 1. Handle Cached Response
        if (cachedFullOllamaJson != null) {
            try {
                // Parse the cached full JSON to extract only the 'response' field
                JsonNode rootNode = objectMapper.readTree(cachedFullOllamaJson);
                extractedResponseText = rootNode.get("response").asText();
                // Return DTO with only the extracted text
                return new LLMResponseDTO(dto.getPrompt(), extractedResponseText);
            } catch (Exception e) {
                // Log the error if cached data is malformed and proceed to fetch from LLM
                System.err.println("Error parsing cached LLM response for prompt '" + dto.getPrompt() + "': " + e.getMessage());
                // Optionally, you might want to return an error DTO or specific message here
                // For now, we'll fall through and re-fetch from the LLM
                extractedResponseText = "Error retrieving cached response. Attempting re-fetch..."; // Temporary message
            }
        }

        // 2. Fetch from LLM if not cached or cache parsing failed
        String fullOllamaJson = llmClient.askLLM(dto.getPrompt()); // This gets the full JSON string from Ollama

        try {
            // Parse the full JSON from Ollama to extract just the 'response' field
            JsonNode rootNode = objectMapper.readTree(fullOllamaJson);
            extractedResponseText = rootNode.get("response").asText();

            // Save the full Ollama JSON response to Redis for future caching
            redisTemplate.opsForValue().set(key, fullOllamaJson, Duration.ofSeconds(30));

        } catch (Exception e) {
            System.err.println("Error parsing LLM response from Ollama for prompt '" + dto.getPrompt() + "': " + e.getMessage());
            // Fallback in case parsing fails (e.g., malformed JSON from Ollama)
            extractedResponseText = "Failed to parse LLM response.";
            // IMPORTANT: If parsing fails, you might not want to cache 'fullOllamaJson'
            // or you might want to handle it differently.
        }

        // 3. Save to Database
        LlmResponse entity = new LlmResponse();
        entity.setPrompt(dto.getPrompt());
        entity.setResponse(extractedResponseText); // <--- Set ONLY the extracted plain text response here

        if (dto.getTaskId() != null) {
            taskRepo.findById(dto.getTaskId()).ifPresent(entity::setTask);
        }
        if (dto.getSubtaskId() != null) {
            subtaskRepo.findById(dto.getSubtaskId()).ifPresent(entity::setSubTask);
        }

        responseRepo.save(entity); // Save the entity with the plain text response

        // 4. Return DTO to Frontend
        return new LLMResponseDTO(dto.getPrompt(), extractedResponseText); // <--- Return DTO with ONLY the plain text
    }

    public List<LlmResponse> getHistoryForTask(Long taskId) {
        return responseRepo.findByTaskId(taskId);
    }

    public List<LlmResponse> getHistoryForSubtask(Long subtaskId) {
        return responseRepo.findBySubTaskId(subtaskId);
    }
}
