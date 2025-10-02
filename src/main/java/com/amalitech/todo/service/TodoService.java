package com.amalitech.todo.service;

import com.amalitech.todo.model.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TodoService {

    private static final Logger log = LoggerFactory.getLogger(TodoService.class);

    private final SnsClient snsClient;
    private final SqsClient sqsClient;
    private final DynamoDbTable<Task> taskTable;

    private final String snsTopicArn;
    private final String sqsQueueUrl;

    public TodoService(DynamoDbEnhancedClient enhancedClient, SnsClient snsClient, SqsClient sqsClient,
                       @Value("${SNS_TOPIC_ARN}") String snsTopicArn,
                       @Value("${SQS_QUEUE_URL}") String sqsQueueUrl,
                       @Value("${TABLE_NAME}") String tableName) {
        this.snsClient = snsClient;
        this.sqsClient = sqsClient;
        this.snsTopicArn = snsTopicArn;
        this.sqsQueueUrl = sqsQueueUrl;
        this.taskTable = enhancedClient.table(tableName, TableSchema.fromBean(Task.class));
    }

    public void subscribeToSnsTopic(String email) {
        try {
            snsClient.subscribe(r -> r.topicArn(snsTopicArn).protocol("email").endpoint(email));
            log.info("Successfully subscribed {}", email);
        } catch (Exception e) {
            log.error("Error subscribing user to SNS topic", e);
        }
    }

    public void sendCancellationMessage(String taskId, String eventId) {
        try {
            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(sqsQueueUrl)
                    .messageBody("{\"taskId\":\"" + taskId + "\"}")
                    .messageGroupId(taskId) // Required for FIFO
                    .messageDeduplicationId(taskId + "-" + eventId) // Ensure idempotency
                    .build());
        } catch (Exception e) {
            log.error("Error sending cancellation message to SQS", e);
        }
    }

    public Set<String> getCancelledTaskIds() {
        try {
            List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(sqsQueueUrl)
                    .maxNumberOfMessages(10)
                    .build()).messages();

            if (messages.isEmpty()) {
                return Set.of();
            }

            Set<String> taskIds = new HashSet<>();
            for (Message msg : messages) {
                // Assuming body is a simple JSON like {"taskId":"..."}
                taskIds.add(msg.body().split("\"")[3]);
                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(sqsQueueUrl)
                        .receiptHandle(msg.receiptHandle())
                        .build());
            }
            return taskIds;

        } catch (Exception e) {
            log.error("Error receiving messages from SQS", e);
            return Set.of();
        }
    }

    public void processTaskExpiry(Map<String, AttributeValue> oldImage) {
        String userId = oldImage.get("userId").getS();
        String taskId = oldImage.get("taskId").getS();
        String description = oldImage.get("description").getS();

        Task expiredTask = new Task();
        expiredTask.setUserId(userId);
        expiredTask.setTaskId(taskId);
        expiredTask.setDescription(description);
        expiredTask.setStatus("Expired");
        expiredTask.setDate(Long.parseLong(oldImage.get("date").getN()));
        expiredTask.setDeadline(Long.parseLong(oldImage.get("deadline").getN()));
        saveTask(expiredTask);

        // Send SNS notification
        try {
            String message = String.format("Your task '%s' has expired.", description);
            snsClient.publish(r -> r.topicArn(snsTopicArn).message(message).subject("Task Expired"));
            log.info("Published expiry notification for task {}", taskId);
        } catch (Exception e) {
            log.error("Error publishing to SNS", e);
        }
    }

    public void saveTask(Task task) {
        taskTable.putItem(task);
    }

    public List<Task> getTasksForUser(String userId) {
        QueryConditional query = QueryConditional.keyEqualTo(Key.builder().partitionValue(userId).build());
        return taskTable.query(query).items().stream().collect(Collectors.toList());
    }

    public Task updateTaskStatus(String userId, String taskId, String status) {
        Key key = Key.builder().partitionValue(userId).sortValue(taskId).build();
        Task task = taskTable.getItem(key);
        if (task != null) {
            task.setStatus(status);
            taskTable.updateItem(task);
        }
        return task;
    }

    public void deleteTask(String userId, String taskId) {
        taskTable.deleteItem(Key.builder().partitionValue(userId).sortValue(taskId).build());
    }
}
