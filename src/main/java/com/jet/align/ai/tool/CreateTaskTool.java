package com.jet.align.ai.tool;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.task.TaskService;
import com.jet.align.task.model.TaskRequest;
import com.jet.align.task.model.TaskResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateTaskTool implements Tool<TaskResponse> {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);


    @Override
    public String name() {
        return "create_task";
    }

    @Override
    public String description() {
        return "Creates a new task for the authenticated user.";
    }

    @Override
    public ToolResult<TaskResponse> execute(ToolContext context) {
        TaskRequest request =
                objectMapper.convertValue(
                        context.arguments(),
                        TaskRequest.class
                );
        TaskResponse response =
                taskService.createTask(
                        request,
                        context.user()
                );
        return new ToolResult<>(
                response,
                "Task created successfully."
        );

    }

    private final TaskService taskService;
    private final ObjectMapper objectMapper;





}
