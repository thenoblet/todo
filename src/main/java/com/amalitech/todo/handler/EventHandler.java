package com.amalitech.todo.handler;

import com.amalitech.todo.service.TodoService;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPostAuthenticationEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

@Component
public class EventHandler {

    private static final Logger log = LoggerFactory.getLogger(EventHandler.class);
    private final TodoService todoService;

    public EventHandler(TodoService todoService) {
        this.todoService = todoService;
    }

    public void handlePostAuth(CognitoUserPoolPostAuthenticationEvent event) {
        String userEmail = event.getRequest().getUserAttributes().get("email");
        log.info("Subscribing user {} to SNS topic", userEmail);
        todoService.subscribeToSnsTopic(userEmail);
    }

    public void handleStream(DynamodbEvent event) {
        for (DynamodbEvent.DynamodbStreamRecord record : event.getRecords()) {
            String eventName = record.getEventName();
            if ("MODIFY".equals(eventName) || "REMOVE".equals(eventName)) {
                String taskId = record.getDynamodb().getKeys().get("taskId").getS();
                String status = record.getDynamodb().getNewImage() != null ?
                        record.getDynamodb().getNewImage().get("status").getS() : null;

                if (("MODIFY".equals(eventName) && "Completed".equals(status)) || "REMOVE".equals(eventName)) {
                    log.info("Task {} was {}d. Sending cancellation message.", taskId, status != null ? status.toLowerCase() : "delete");
                    todoService.sendCancellationMessage(taskId, record.getEventID());
                }
            }
        }
    }

    public void handleExpiry(DynamodbEvent event) {
        var cancelledTaskIds = todoService.getCancelledTaskIds();

        for (DynamodbEvent.DynamodbStreamRecord record : event.getRecords()) {
            String taskId = record.getDynamodb().getOldImage().get("taskId").getS();
            if (cancelledTaskIds.contains(taskId)) {
                log.info("Expiry for task {} was cancelled. Skipping notification.", taskId);
                continue;
            }
            log.info("Task {} has expired. Processing expiry.", taskId);
            todoService.processTaskExpiry(record.getDynamodb().getOldImage());
        }
    }
}

