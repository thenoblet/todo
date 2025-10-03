package com.amalitech.todo.service;

import com.amalitech.todo.model.Task;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NotificationService {
    private final SnsClient snsClient;
    private final SqsClient sqsClient;
    private final String snsTopicArn = System.getenv("SNS_TOPIC_ARN");
    private final String sqsQueueUrl = System.getenv("SQS_QUEUE_URL");
    private final ObjectMapper objectMapper = new ObjectMapper();


    public NotificationService() {
        this.snsClient = SnsClient.builder().build();
        this.sqsClient = SqsClient.builder().build();
    }

    public void subscribeUserToNotifications(String email) {
        SubscribeRequest request = SubscribeRequest.builder()
                .protocol("email")
                .endpoint(email)
                .topicArn(snsTopicArn)
                .build();
        snsClient.subscribe(request);
    }

    public void sendTaskExpiryNotification(Task task) {
        String subject = "Task Deadline approaching: " + task.getTitle();
        String message = String.format(
                "Your task '%s' is due soon!\n\nDescription: %s",
                task.getTitle(),
                task.getDescription()
        );

        PublishRequest request = PublishRequest.builder()
                .topicArn(snsTopicArn)
                .subject(subject)
                .message(message)
                .build();
        snsClient.publish(request);
    }

    public void scheduleTaskForExpiryCheck(Task task) {
        try {
            String messageBody = objectMapper.writeValueAsString(task);
            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(sqsQueueUrl)
                    .messageBody(messageBody)
                    .messageGroupId(task.getUserId())
                    .messageDeduplicationId(task.getTaskId() + "-" + System.currentTimeMillis())
                    .build();
            sqsClient.sendMessage(request);
        } catch (Exception e) {
            System.err.println("Failed to send message to SQS: " + e.getMessage());
        }
    }
}
