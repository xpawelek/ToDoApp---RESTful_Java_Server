package efs.task.todoapp;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class TaskService implements HttpHandler {
    UserService userService;
    AuthService authService;
    private final TaskRepository repository;
    private final TaskParser taskParser;
    private final AppLogger logger;

    private final Set<Task> tasks;

    public TaskService(UserService userService, TaskRepository repository, TaskParser taskParser, AppLogger logger) {
        this.userService = userService;
        this.authService = new AuthService(userService);
        this.repository = repository;
        tasks = repository.loadTasks();
        this.taskParser = taskParser;
        this.logger = logger;
    }
    @Override
    public void handle(HttpExchange exchange) throws IOException
    {
        String method = exchange.getRequestMethod();

        switch (method) {
            case "GET":
                handleGet(exchange);
                break;
            case "POST":
                handlePost(exchange);
                break;
            case "PUT":
                handlePut(exchange);
                break;
            case "DELETE":
                handleDelete(exchange);
                break;
            default:
                exchange.sendResponseHeaders(405, -1);
                break;
        }
    }

    private void handleGet(HttpExchange exchange) throws IOException
    {
        //200 return data
        //400 - some error??
        //401 - invalid authentication
        String formattedData = null;
        String query = exchange.getRequestURI().getQuery();
        if (query != null) {
            if(query.contains("id="))
            {
                Task taskToReturn = returnTaskByFilter(exchange, query, "id").stream()
                        .findFirst()
                        .orElse(null);
                ;
                if(taskToReturn == null) {
                    sendResponse(exchange, "Task not found", 404);
                    logger.log("Task not found");
                    return;
                }
                formattedData = taskParser.serializeTask(taskToReturn);
            }
            else if (query.contains("status="))
            {
                List<Task> tasksByStatus = returnTaskByFilter(exchange, query, "status");
                if(tasksByStatus == null) {
                    return;
                }
                formattedData = taskParser.serializeTask(tasksByStatus);
            }

            sendResponse(exchange, formattedData, 200);
            logger.log("Get task executed: \\todo\\task, status code 200");
        }
        else {
            returnAllTasks(exchange);
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException
    {
        String requestBody = readRequestBody(exchange);

        Task newTask;
        try
        {
            newTask = taskParser.parseTask(requestBody);
        } catch (Exception e)
        {
            sendResponse(exchange, "Invalid Format Input", 400);
            logger.log("Invalid Format Input");
            return;
        }

        if(!checkInputValidity(newTask.getDescription(), newTask.getDue()) || isInPast(newTask.getDue()))
        {
            sendResponse(exchange, "Invalid data provided", 400);
            logger.log("Invalid data provided");
            return;
        }

        if(!authService.isAuthenticated(exchange))
        {
            sendResponse(exchange, "Invalid authentication", 401);
            logger.log("Invalid authentication");
            return;
        }

        Map.Entry<String, String> creds = authService.getUserDataFromAuthentication(exchange);
        String owner = creds.getKey();
        newTask.setOwner(owner);
        tasks.add(newTask);

        repository.saveTasks(tasks);
        sendResponse(exchange, "New task added with id = " + newTask.getId(), 201);
        logger.log("New task has been added for owner: " + owner + "! Status code 201");
    }

    private void handlePut(HttpExchange exchange) throws IOException {

        //json upadate to be more open
        String requestBody = readRequestBody(exchange);

        String query = exchange.getRequestURI().getQuery();
        if (query == null) {
            sendResponse(exchange, "Bad request", 400);
            return;
        }

        Task taskToModify = returnTaskByFilter(exchange, query, "id").stream()
                .findFirst()
                .orElse(null);;

        if (taskToModify == null) {
            sendResponse(exchange, "Task not found", 404);
            return;
        }

        Task updatedTask;
        try {
            updatedTask = taskParser.parseTask(requestBody);
        } catch (Exception e) {
            sendResponse(exchange, "Invalid Format Input", 400);
            return;
        }

        if (isNullOrBlank(updatedTask.getDescription())  &&
                isNullOrBlank(updatedTask.getDue()) &&
                isNullOrBlank(updatedTask.getStatus().toString())) {
            sendResponse(exchange, "No data to update", 400);
            return;
        }

        if (!isNullOrBlank(updatedTask.getDescription()))
            taskToModify.setDescription(updatedTask.getDescription());

        if (!isNullOrBlank(updatedTask.getDue())) {
            if (!isValidISODate(updatedTask.getDue())) {
                sendResponse(exchange, "Invalid due date format", 400);
                return;
            }
            taskToModify.setDue(updatedTask.getDue());
        }

        if (!isNullOrBlank(updatedTask.getStatus().toString()))
            taskToModify.setStatus(updatedTask.getStatus());


        String resp_ = taskParser.serializeTask(taskToModify);
        sendResponse(exchange, resp_, 200);
        logger.log("Task has been updated with id = " + updatedTask.getId() + "! Status code 200");
    }


    private void handleDelete(HttpExchange exchange) throws IOException
    {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) {
            sendResponse(exchange, "Bad request", 400);
            return;
        }

        Task taskToDelete = returnTaskByFilter(exchange, query, "id").stream()
                .findFirst()
                .orElse(null);;

        if (taskToDelete == null) {
            sendResponse(exchange, "Task not found", 404);
            return;
        }

        tasks.remove(taskToDelete);
        repository.saveTasks(tasks);
        sendResponse(exchange, "Task with id: " + taskToDelete.getId() + " has been deleted", 200);
        logger.log("Task has been deleted with id = " + taskToDelete.getId());
    }

    private void returnAllTasks(HttpExchange exchange) throws IOException
    {
        if(!authService.isAuthenticated(exchange)) {
            sendResponse(exchange, "User not authenticated", 401);
            logger.log("User not authenticated");
            return;
        }

        Map.Entry<String, String> creds = authService.getUserDataFromAuthentication(exchange);
        String username = creds.getKey();

        List<Task> userTasks = tasks.stream()
                .filter(task -> username.equals(task.getOwner()))
                .collect(Collectors.toList());

        String formattedData = taskParser.serializeTask(userTasks);
        sendResponse(exchange, formattedData, 200);
        logger.log("All tasks for user: " + username + " has been returned");
    }

    private List<Task> returnTaskByFilter(HttpExchange exchange, String query, String filter) throws IOException
    {
        if(!query.contains(filter))
        {
            sendResponse(exchange, "Bad request", 400);
            logger.log("Bad request, status code 400");
            return null;
        }

        if(!authService.isAuthenticated(exchange)) {
            sendResponse(exchange, "User not authenticated", 401);
            logger.log("User not authenticated, status code 401");
            return null;
        }

        Map<String, String> filterKey = parseQuery(query);
        Map.Entry<String, String> creds = authService.getUserDataFromAuthentication(exchange);
        String username = creds.getKey();

        List<Task> tasksToReturn = null;
        switch(filter)
        {
            case "status":
                tasksToReturn = tasks.stream()
                        .filter(task -> task.getStatus().name().equalsIgnoreCase(filterKey.get("status")))
                        .collect(Collectors.toList());
                break;
            case "id":
                Task foundTask = tasks.stream()
                        .filter(task -> task.getId().equals(filterKey.get("id")))
                        .findFirst()
                        .orElse(null);
                tasksToReturn = foundTask != null ? List.of(foundTask) : List.of();
                break;
        }

        if (tasksToReturn == null) {
            sendResponse(exchange, "Task not found", 404);
            logger.log("Task not found, status code 404");
            return null;
        }

        for(Task task : tasksToReturn)
        {
            if (!username.equals(task.getOwner())) {
                sendResponse(exchange, "Data forbidden for user!", 403);
                logger.log("Data forbidden for user! Status code 403");
                return null;
            }
        }

        logger.log("Data gonna be returned based on filter: " + filter);
        return tasksToReturn;
    }

    private void sendResponse(HttpExchange exchange, String response, int statusCode) throws IOException {
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode,responseBytes.length);

        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
            return br.lines().collect(Collectors.joining());
        }
    }

    private boolean isValidISODate(String dateString) {
        try {
            LocalDate.parse(dateString, DateTimeFormatter.ISO_DATE);
            return true; // Valid ISO format (yyyy-MM-dd)
        } catch (DateTimeParseException e) {
            return false; // Invalid format
        }
    }

    private boolean isNullOrBlank(String str) {
        return str == null || str.isBlank();
    }

    private boolean isInPast(String dateString) {
        LocalDate date = LocalDate.parse(dateString);
        LocalDate today = LocalDate.now();
        return date.isBefore(today);
    }

    private boolean checkInputValidity(String description, String due) {
        if(isNullOrBlank(description) || !isValidISODate(due) || isNullOrBlank(due)) {
            return false;
        }
        return true;
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> parsedQuery = new HashMap<>();

        if(query == null || query.isBlank()) return parsedQuery;

        String[] parts = query.split("&");
        for(String part : parts) {
            String[] keyValue = part.split("=", 2);
            if(keyValue.length == 2) {
                parsedQuery.put(keyValue[0], keyValue[1]);
            }
            else if (keyValue.length == 1) {
                parsedQuery.put(keyValue[0], "");
            }
        }
        return parsedQuery;
    }
}
