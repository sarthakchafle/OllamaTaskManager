package com.agentic.taskmanager.Model;

import jakarta.persistence.*;
import org.springframework.cache.annotation.EnableCaching;

import java.time.LocalDateTime;

@Entity
public class SubTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String step;

    @ManyToOne
    @JoinColumn(name = "task_id")
    private Task task;

    @OneToOne(mappedBy = "subTask", cascade = CascadeType.ALL)
    private LlmResponse llmResponse;

    private LocalDateTime createdAt = LocalDateTime.now();
}
