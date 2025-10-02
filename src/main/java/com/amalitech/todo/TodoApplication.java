package com.amalitech.todo;

import com.amalitech.todo.controller.ApiController;
import com.amalitech.todo.handler.EventHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPostAuthenticationEvent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.function.Function;

@SpringBootApplication
public class TodoApplication {

    public static void main(String[] args) {
        SpringApplication.run(TodoApplication.class, args);
    }


    @Bean
    public Function<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> handleApi(ApiController controller) {
        return controller::handleRequest;
    }

    @Bean
    public Function<DynamodbEvent, Void> handleStream(EventHandler handler) {
        return event -> {
            handler.handleStream(event);
            return null;
        };
    }

    @Bean
    public Function<DynamodbEvent, Void> handleExpiry(EventHandler handler) {
        return event -> {
            handler.handleExpiry(event);
            return null;
        };
    }

    @Bean
    public Function<CognitoUserPoolPostAuthenticationEvent, Void> handlePostAuth(EventHandler handler) {
        return event -> {
            handler.handlePostAuth(event);
            return null;
        };
    }
}
