package com.amalitech.todo.handler;

import com.amalitech.todo.model.Task;
import com.amalitech.todo.service.TaskService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;

public class ExpiryHandler {

    private final TaskService taskService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExpiryHandler() {
        this.taskService = new TaskService();
    }

    /**
     * This function is triggered by the DynamoDB stream.
     * Its job is to send a message to SQS for any new or updated task with a deadline.
     */
    public void processStream(DynamodbEvent event, Context context) {
        for (DynamodbEvent.DynamodbStreamRecord record : event.getRecords()) {
            if (record.getEventName().equals("INSERT") || record.getEventName().equals("MODIFY")) {
                StreamRecord streamRecord = record.getDynamodb();
                Map<String, AttributeValue> newImage = streamRecord.getNewImage();

                Task task = new Task();
                task.setUserId(newImage.get("userId").getS());
                task.setTaskId(newImage.get("taskId").getS());
                task.setTitle(newImage.get("title").getS());
                task.setDescription(newImage.get("description").getS());
                task.setCompleted(newImage.get("completed").getBOOL());
                task.setDeadline(Long.parseLong(newImage.get("deadline").getN()));

                context.getLogger().log("Processing stream record for task ID: " + task.getTaskId());
                taskService.scheduleExpiryCheck(task);
            }
        }
    }

    /**
     * This function is triggered by SQS.
     * It processes a task from the queue and sends a notification if it's near expiry.
     */
    public void handleExpiry(SQSEvent event, Context context) {
        for (SQSEvent.SQSMessage msg : event.getRecords()) {
            try {
                Task task = objectMapper.readValue(msg.getBody(), Task.class);
                context.getLogger().log("Handling expiry for task ID: " + task.getTaskId());

                long now = Instant.now().getEpochSecond();

                if (task.getDeadline() > 0 && task.getDeadline() < now) {
                    taskService.processTaskExpiry(task);
                } else {
                    context.getLogger().log("Task " + task.getTaskId() + " is not yet expired.");
                }

            } catch (Exception e) {
                context.getLogger().log("Error processing SQS message: " + e.getMessage());
                throw new RuntimeException("Could not process message", e);
            }
        }
    }
}

