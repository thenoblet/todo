package com.amalitech.todo.controller;

import com.amalitech.todo.model.Task;
import com.amalitech.todo.service.TodoService;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Component
public class ApiController {

    private static final Logger log = LoggerFactory.getLogger(ApiController.class);
    private final TodoService todoService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiController(TodoService todoService) {
        this.todoService = todoService;
    }

    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request) {
        try {
            Map<String, String> claims = (Map<String, String>) request.getRequestContext().getAuthorizer().get("claims");
            String userId = claims.get("sub");

            HttpMethod method = HttpMethod.valueOf(request.getHttpMethod());
            String path = request.getPath();

            if (path.equals("/tasks")) {
                if (method == HttpMethod.POST) {
                    return createTask(userId, request.getBody());
                } else if (method == HttpMethod.GET) {
                    return getTasks(userId);
                }
            } else if (path.startsWith("/tasks/")) {
                String taskId = path.substring(path.lastIndexOf('/') + 1);
                if (method == HttpMethod.PUT) {
                    return updateTask(userId, taskId, request.getBody());
                } else if (method == HttpMethod.DELETE) {
                    return deleteTask(userId, taskId);
                }
            }

            return createResponse(404, Map.of("message", "Not Found: " + method + " " + path));
        } catch (Exception e) {
            log.error("Error processing API request", e);
            return createResponse(500, Map.of("message", "Internal Server Error"));
        }
    }

    private APIGatewayProxyResponseEvent createTask(String userId, String body) throws JsonProcessingException {
        Task task = objectMapper.readValue(body, Task.class);
        task.setUserId(userId);
        task.setTaskId(UUID.randomUUID().toString());
        task.setStatus("Pending");
        task.setDate(Instant.now().getEpochSecond());
        task.setDeadline(Instant.now().plus(5, ChronoUnit.MINUTES).getEpochSecond());

        todoService.saveTask(task);
        return createResponse(201, task);
    }

    private APIGatewayProxyResponseEvent getTasks(String userId) {
        var tasks = todoService.getTasksForUser(userId);
        return createResponse(200, tasks);
    }

    private APIGatewayProxyResponseEvent updateTask(String userId, String taskId, String body) throws JsonProcessingException {
        Map<String, String> payload = objectMapper.readValue(body, Map.class);
        String status = payload.get("status");
        if (!"Completed".equals(status)) {
            return createResponse(400, Map.of("message", "Invalid status. Only 'Completed' is allowed."));
        }
        Task updatedTask = todoService.updateTaskStatus(userId, taskId, status);
        return createResponse(200, updatedTask);
    }

    private APIGatewayProxyResponseEvent deleteTask(String userId, String taskId) {
        todoService.deleteTask(userId, taskId);
        return createResponse(204, null);
    }

    private APIGatewayProxyResponseEvent createResponse(int statusCode, Object body) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);
        response.setHeaders(Collections.singletonMap("Access-Control-Allow-Origin", "*"));
        if (body != null) {
            try {
                response.setBody(objectMapper.writeValueAsString(body));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize response body", e);
                response.setStatusCode(500);
                response.setBody("{\"message\":\"Internal server error\"}");
            }
        }
        return response;
    }
}

