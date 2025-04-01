package efs.task.todoapp;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Base64;
import java.util.Map;

public class AuthService {
    private final UserService userService;

    public AuthService(UserService userService) {
        this.userService = userService;
    }

    public Map.Entry<String, String> getUserDataFromAuthentication(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return null;
        }

        String encodedCredentials = authHeader.substring("Basic ".length());
        String decodedCredentials = new String(Base64.getDecoder().decode(encodedCredentials), StandardCharsets.UTF_8);

        String[] credentialsArr = decodedCredentials.split(":");
        if (credentialsArr.length != 2) {
            return null;
        }

        Map.Entry<String, String> credentials = new AbstractMap.SimpleEntry<>(credentialsArr[0], credentialsArr[1]);
        return credentials;
    }

    public boolean isAuthenticated(HttpExchange exchange) throws IOException {
        Map.Entry<String, String> creds = getUserDataFromAuthentication(exchange);
        return creds != null && userService.userExists(creds.getKey(), creds.getValue());
    }
}
