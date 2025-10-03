package com.amalitech.todo.handler;

import com.amalitech.todo.service.NotificationService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPostAuthenticationEvent;


public class AuthHandler implements RequestHandler<CognitoUserPoolPostAuthenticationEvent, CognitoUserPoolPostAuthenticationEvent> {

    private final NotificationService notificationService;

    public AuthHandler() {
        this.notificationService = new NotificationService();
    }

    @Override
    public CognitoUserPoolPostAuthenticationEvent handleRequest(CognitoUserPoolPostAuthenticationEvent event, Context context) {
        String email = event.getRequest().getUserAttributes().get("email");

        if (email != null && !email.isEmpty()) {
            context.getLogger().log("Subscribing user to SNS topic: " + email);
            notificationService.subscribeUserToNotifications(email);
        } else {
            context.getLogger().log("User does not have an email attribute, skipping subscription.");
        }

        return event;
    }
}

