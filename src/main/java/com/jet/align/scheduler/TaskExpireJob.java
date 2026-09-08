package com.jet.align.scheduler;

import com.jet.align.task.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskExpireJob {

    private final TaskService taskService;


    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void run() {
        taskService.expireOverdueTasks();
    }


}
