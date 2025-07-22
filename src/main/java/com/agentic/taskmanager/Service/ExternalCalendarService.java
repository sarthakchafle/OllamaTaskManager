package com.agentic.taskmanager.Service;

import com.agentic.taskmanager.Feign.TaskProcessorClient;
import com.agentic.taskmanager.Model.CalendarResponse;
import com.agentic.taskmanager.Model.TaskRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExternalCalendarService {
    @Autowired
    private final TaskProcessorClient taskProcessorClient;

    public ExternalCalendarService(TaskProcessorClient taskProcessorClient) {
        this.taskProcessorClient = taskProcessorClient;
    }

    public CalendarResponse processRequest(TaskRequest taskRequest) {
        String response = taskProcessorClient.processTask(taskRequest);
        return new CalendarResponse(response);
    }
}
