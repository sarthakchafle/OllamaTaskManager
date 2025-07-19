package com.agentic.taskmanager.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class LlmResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String prompt;
    @Column(columnDefinition = "TEXT")
    @Lob
    private String response;
    @ManyToOne
    private Task task;
    @OneToOne
    @JoinColumn(name = "sub_task_id")
    private SubTask subTask;

    private LocalDateTime createdAt = LocalDateTime.now();
}
