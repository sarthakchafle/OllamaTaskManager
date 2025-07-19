package com.agentic.taskmanager.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LlmRequestDTO {
    private String prompt;
    private Long taskId;
    private Long subtaskId;
}
