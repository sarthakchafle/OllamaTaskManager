package com.agentic.taskmanager.Controller;

import com.agentic.taskmanager.DTO.LLMResponseDTO;
import com.agentic.taskmanager.DTO.LlmRequestDTO;
import com.agentic.taskmanager.Feign.TaskProcessorClient;
import com.agentic.taskmanager.Model.CalendarResponse;
import com.agentic.taskmanager.Model.LlmResponse;
import com.agentic.taskmanager.Model.TaskRequest;
import com.agentic.taskmanager.Service.ExternalCalendarService;
import com.agentic.taskmanager.Service.LlmService;
import io.netty.handler.timeout.ReadTimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class LlmController {

    // Use constructor injection without @Autowired on fields
    private final LlmService service;
    private final ExternalCalendarService externalCalendarService;

    public LlmController(LlmService service, ExternalCalendarService externalCalendarService) {
        this.service = service;
        this.externalCalendarService = externalCalendarService;
    }

    @PostMapping("/ask")
    public ResponseEntity<?> ask(@RequestBody LlmRequestDTO dto) {
        String prompt = dto.getPrompt();
        if (prompt.contains("schedule meeting") ||
                prompt.contains("plan meeting") ||
                prompt.contains("schedule") ||
                prompt.contains("meeting") ||
                prompt.contains("delete meeting") || // Added for delete functionality
                prompt.contains("cancel meeting")||
        prompt.contains("delete event")) { // Added for delete functionality
            System.out.println("Calendar service called via ExternalCalendarService");

            // Correct TaskRequest instantiation for the Canvas's TaskRequest model
            TaskRequest taskRequest = new TaskRequest(prompt);

            // Call the ExternalCalendarService to process the request
            try {
                CalendarResponse calendarResponse = externalCalendarService.processRequest(taskRequest);
                return ResponseEntity.ok(calendarResponse.getResponse());
            }
            catch (ReadTimeoutException e) {
                return ResponseEntity.status(408).body("Service took time to respond, please check manually in the calendar if the event if created/deleted.");
            }
            catch (Exception e){
                return ResponseEntity.status(500).body(e.getMessage());

            }
        }
        System.out.println("Local service called");
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

