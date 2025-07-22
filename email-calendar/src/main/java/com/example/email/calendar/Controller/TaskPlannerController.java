package com.example.email.calendar.Controller;

import com.example.email.calendar.Model.TaskRequest;
import com.example.email.calendar.Model.TaskResponse;
import com.example.email.calendar.Services.TaskPlannerService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/orchestrator")
public class TaskPlannerController {
    private final TaskPlannerService service; // Updated service type

    @Autowired
    public TaskPlannerController(TaskPlannerService service) { // Updated constructor
        this.service = service;
    }

    /**
     * Endpoint to trigger the workflow (email sending or calendar event creation)
     * based on a user prompt.
     * Example usage (POST request with JSON body):
     *
     * To send an email:
     * URL: http://localhost:8082/api/orchestrator/process-prompt
     * Body (LLM extracts details):
     * {
     * "prompt": "Send an email to john.doe@example.com with subject 'Meeting Reminder' and body 'Don't forget our meeting tomorrow at 10 AM.'"
     * }
     * Body (explicit details):
     * {
     * "prompt": "Send a quick note.",
     * "recipientEmail": "jane.doe@example.com",
     * "emailSubject": "Quick Check-in",
     * "emailBody": "Just wanted to say hi!" // Note: LLM can also extract this from prompt
     * }
     *
     * To create a calendar event:
     * URL: http://localhost:8082/api/orchestrator/process-prompt
     * Body:
     * {
     * "prompt": "Set up a meeting for Project Alpha review tomorrow at 2 PM for 1 hour. It's about discussing Q3 results."
     * }
     *
     * @param request The user's prompt and optional details for email.
     * @return A response indicating the status of the operation.
     */
    @PostMapping("/process-prompt")
    public ResponseEntity<TaskResponse> processUserPrompt(@RequestBody TaskRequest request) {
        String result = service.processUserPrompt(request.getPrompt());

        TaskResponse taskResponse = new TaskResponse(result);

        if (result.startsWith("Error")) {
            return ResponseEntity.status(500).body(taskResponse);
        }

        return ResponseEntity.ok(taskResponse);
    }
}
