package com.amalitech.todo.dto.request;

/**
 * Represents the JSON payload for a create task API request.
 */
public record CreateTaskRequest(
        String title,
        String description,
        long deadline
) {}
