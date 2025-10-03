package com.amalitech.todo.service;

import com.amalitech.todo.model.Task;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;

public class DynamoDBService {

    private final DynamoDbTable<Task> tasksTable;

    public DynamoDBService() {
        DynamoDbClient ddb = DynamoDbClient.builder()
                .region(Region.EU_CENTRAL_1)
                .build();

        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(ddb)
                .build();

        this.tasksTable = enhancedClient.table("TaskTable",
                TableSchema.fromBean(Task.class));
    }

    public void saveTask(Task task) {
        tasksTable.putItem(task);
    }

    public List<Task> getTasksForUser(String userId) {
        QueryConditional queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(userId).build());
        return tasksTable.query(queryConditional).items().stream().toList();
    }

    public Task getTask(String userId, String taskId) {
        Key key = Key.builder().partitionValue(userId).sortValue(taskId).build();
        return tasksTable.getItem(key);
    }

    public void updateTask(Task task) {
        tasksTable.updateItem(task);
    }

    public void deleteTask(String userId, String taskId) {
        Key key = Key.builder().partitionValue(userId).sortValue(taskId).build();
        tasksTable.deleteItem(key);
    }
}
