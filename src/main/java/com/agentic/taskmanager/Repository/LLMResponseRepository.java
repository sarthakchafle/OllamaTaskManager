package com.agentic.taskmanager.Repository;

import com.agentic.taskmanager.Model.LlmResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LLMResponseRepository extends JpaRepository<LlmResponse,Long> {
    Optional<LlmResponse> findByPrompt(String prompt);
    List<LlmResponse> findByTaskId(Long taskId);
    List<LlmResponse> findBySubTaskId(Long subTaskId);
}
