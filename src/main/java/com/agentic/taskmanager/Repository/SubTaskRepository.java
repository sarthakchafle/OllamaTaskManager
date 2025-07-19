package com.agentic.taskmanager.Repository;

import com.agentic.taskmanager.Model.SubTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubTaskRepository extends JpaRepository<SubTask,Long> {
}
