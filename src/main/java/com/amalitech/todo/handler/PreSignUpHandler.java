package com.amalitech.todo.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.Map;

@SuppressWarnings("unchecked")
public class PreSignUpHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        context.getLogger().log("PreSignUp event: " + event);

        Map<String, Object> response = (Map<String, Object>) event.get("response");
        if (response == null) {
            response = new java.util.HashMap<>();
            event.put("response", response);
        }

        response.put("autoConfirmUser", true);
        response.put("autoVerifyEmail", true);

        return event;
    }
}
