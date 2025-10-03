package com.amalitech.todo.handler;

import com.amalitech.todo.dto.request.CreateTaskRequest;
import com.amalitech.todo.dto.request.UpdateTaskRequest;
import com.amalitech.todo.model.Task;
import com.amalitech.todo.service.TaskService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskHandler {

    private final TaskService taskService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TaskHandler() {
        this.taskService = new TaskService();
    }

    private Map<String, String> getCorsHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "https://todoapp.d36509gvuetnkq.amplifyapp.com");
        headers.put("Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token");
        headers.put("Access-Control-Allow-Methods", "OPTIONS,POST,GET,PUT,DELETE");
        return headers;
    }

    private String getUserId(APIGatewayProxyRequestEvent request) {
        try {
            Map<String, Object> authorizer = request.getRequestContext().getAuthorizer();
            if (authorizer == null) {
                throw new RuntimeException("Authorizer not found in request");
            }

            Object claimsObject = authorizer.get("claims");
            if (claimsObject instanceof Map) {
                Map<?, ?> claims = (Map<?, ?>) claimsObject;
                Object sub = claims.get("sub");
                if (sub != null) {
                    return sub.toString();
                }
            }
            throw new RuntimeException("User ID (sub) not found in claims");
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract user ID from request: " + e.getMessage(), e);
        }
    }

    public APIGatewayProxyResponseEvent createTask(APIGatewayProxyRequestEvent request, Context context) {
        try {
            String userId = getUserId(request);
            CreateTaskRequest createTaskRequest = objectMapper.readValue(request.getBody(), CreateTaskRequest.class);
            Task createdTask = taskService.createTask(userId, createTaskRequest);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(201)
                    .withHeaders(getCorsHeaders())
                    .withBody(objectMapper.writeValueAsString(createdTask));
        } catch (Exception e) {
            context.getLogger().log("Error creating task: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withHeaders(getCorsHeaders())
                    .withBody("{\"error\":\"Could not create task\"}");
        }
    }

    public APIGatewayProxyResponseEvent getTasks(APIGatewayProxyRequestEvent request, Context context) {
        try {
            String userId = getUserId(request);
            List<Task> tasks = taskService.getTasks(userId);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(getCorsHeaders())
                    .withBody(objectMapper.writeValueAsString(tasks));
        } catch (Exception e) {
            context.getLogger().log("Error getting tasks: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withHeaders(getCorsHeaders())
                    .withBody("{\"error\":\"Could not retrieve tasks\"}");
        }
    }

    public APIGatewayProxyResponseEvent updateTask(APIGatewayProxyRequestEvent request, Context context) {
        try {
            String userId = getUserId(request);
            String taskId = request.getPathParameters().get("taskId");
            UpdateTaskRequest updateTaskRequest = objectMapper.readValue(request.getBody(), UpdateTaskRequest.class);
            Task updatedTask = taskService.updateTask(userId, taskId, updateTaskRequest);

            if (updatedTask == null) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(404)
                        .withHeaders(getCorsHeaders())
                        .withBody("{\"error\":\"Task not found\"}");
            }

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(getCorsHeaders())
                    .withBody(objectMapper.writeValueAsString(updatedTask));
        } catch (Exception e) {
            context.getLogger().log("Error updating task: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withHeaders(getCorsHeaders())
                    .withBody("{\"error\":\"Could not update task\"}");
        }
    }

    public APIGatewayProxyResponseEvent deleteTask(APIGatewayProxyRequestEvent request, Context context) {
        try {
            String userId = getUserId(request);
            String taskId = request.getPathParameters().get("taskId");
            boolean deleted = taskService.deleteTask(userId, taskId);

            if (!deleted) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(404)
                        .withHeaders(getCorsHeaders())
                        .withBody("{\"error\":\"Task not found\"}");
            }

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(204)
                    .withHeaders(getCorsHeaders())
                    .withBody("");
        } catch (Exception e) {
            context.getLogger().log("Error deleting task: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withHeaders(getCorsHeaders())
                    .withBody("{\"error\":\"Could not delete task\"}");
        }
    }
}
