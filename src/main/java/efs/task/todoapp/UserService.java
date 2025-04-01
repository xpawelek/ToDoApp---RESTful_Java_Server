package efs.task.todoapp;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class UserService implements HttpHandler {
    private final Set<User> existingUsers = new HashSet<>();
    private final Gson gson = new Gson();
    private final AppLogger logger;

    public UserService(AppLogger logger) {
        this.logger = logger;
    }


    @Override
    public void handle(HttpExchange exchange) throws IOException
    {
        String method = exchange.getRequestMethod();

        if(method.equals("POST"))
        {
            handlePost(exchange);
        }
        else
        {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        String line;
        while( (line = br.readLine()) != null)
        {
            builder.append(line);
        }

        User newUser = null;
        try {
            newUser = gson.fromJson(builder.toString(), User.class);
        } catch (Exception e) {
            sendResponse(exchange, "Invalid JSON Format", 400);
            return;
        }

        if(newUser.getUsername() == null || newUser.getPassword() == null || newUser.getUsername().isBlank() ||
                newUser.getPassword().isBlank() || newUser.getPassword().isBlank())
        {
            sendResponse(exchange, "Missing username or password", 400);
            return;
        }

        User finalUser = newUser;
        boolean userExists = existingUsers.stream().anyMatch(user -> user.getUsername().equals(finalUser.getUsername()));

        if(userExists) {
            sendResponse(exchange, "User already exists", 409);
            return;
        }

        existingUsers.add(newUser);
        logger.log("User added: " + finalUser.getUsername());
        sendResponse(exchange, "User created: " + newUser.getUsername(), 201);
    }


    private void sendResponse(HttpExchange exchange, String response, int statusCode) throws IOException {
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        OutputStream os = exchange.getResponseBody();

        os.write(responseBytes);
        os.close();
    }

    public boolean userExists(String username, String password) {
        boolean exists = existingUsers.stream().anyMatch(user -> user.getUsername().equals(username) && user.getPassword().equals(password));
        return exists;
    }
}
