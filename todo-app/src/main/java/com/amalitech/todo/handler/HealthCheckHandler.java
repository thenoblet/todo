package com.amalitech.todo.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.HashMap;
import java.util.Map;

public class HealthCheckHandler {
    private Map<String, String> getCorsHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "https://todoapp.d36509gvuetnkq.amplifyapp.com");
        headers.put("Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token,X-Requested-With");
        headers.put("Access-Control-Allow-Methods", "OPTIONS,POST,GET,PUT,DELETE");
        return headers;
    }

    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(getCorsHeaders())
                .withBody("{\"status\":\"healthy\",\"message\":\"API is working\"}");
    }
}
