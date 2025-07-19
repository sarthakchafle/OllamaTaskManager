package com.agentic.taskmanager.DTO;

import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LLMResponseDTO {
    private String prompt;
    @Column(columnDefinition = "TEXT")
    @Lob
    private String response;

    public LLMResponseDTO(String prompt, String response) {
        this.prompt = prompt;
        this.response = response;
    }
}
