package com.amalitech.todo.service;

import com.amalitech.todo.dto.request.CreateTaskRequest;
import com.amalitech.todo.dto.request.UpdateTaskRequest;
import com.amalitech.todo.model.Task;

import java.util.List;
import java.util.UUID;

public class TaskService {

    private final DynamoDBService dynamoDBService;
    private final NotificationService notificationService;

    public TaskService() {
        this.dynamoDBService = new DynamoDBService();
        this.notificationService = new NotificationService();
    }

    public Task createTask(String userId, CreateTaskRequest request) {
        Task task = new Task();
        task.setUserId(userId);
        task.setTaskId(UUID.randomUUID().toString());
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setCompleted(false);
        task.setDeadline(request.deadline());
        task.setCreatedAt(System.currentTimeMillis());

        dynamoDBService.saveTask(task);
        return task;
    }

    public List<Task> getTasks(String userId) {
        return dynamoDBService.getTasksForUser(userId);
    }

    public Task updateTask(String userId, String taskId, UpdateTaskRequest request) {
        Task existingTask = dynamoDBService.getTask(userId, taskId);
        if (existingTask == null) {
            return null;
        }

        if (request.title() != null) {
            existingTask.setTitle(request.title());
        }
        if (request.description() != null) {
            existingTask.setDescription(request.description());
        }
        if (request.completed() != null) {
            existingTask.setCompleted(request.completed());
        }
        if (request.deadline() != null) {
            existingTask.setDeadline(request.deadline());
        }

        dynamoDBService.updateTask(existingTask);
        return existingTask;
    }

    public boolean deleteTask(String userId, String taskId) {
        Task task = dynamoDBService.getTask(userId, taskId);
        if (task == null) {
            return false;
        }
        dynamoDBService.deleteTask(userId, taskId);
        return true;
    }

    public void processTaskExpiry(Task task) {
        Task currentTaskState = dynamoDBService.getTask(task.getUserId(), task.getTaskId());

        if (currentTaskState != null && !currentTaskState.isCompleted()) {
            notificationService.sendTaskExpiryNotification(currentTaskState);
        }
    }

    public void scheduleExpiryCheck(Task task) {
        if (task.getDeadline() > 0) {
            notificationService.scheduleTaskForExpiryCheck(task);
        }
    }
}

