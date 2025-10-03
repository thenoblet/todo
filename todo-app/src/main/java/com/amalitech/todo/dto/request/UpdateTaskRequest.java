package com.amalitech.todo.dto.request;

/**
 * Represents the JSON payload for an update task API request.
 */
public record UpdateTaskRequest(
        String title,
        String description,
        Boolean completed,
        Long deadline
) {}
