package efs.task.todoapp;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class TaskServiceTest {

    private TaskService taskService;
    private TaskParser taskParser;
    private UserService userService;
    private TaskRepository taskRepository;
    private AppLogger logger;
    private ToDoApplication app;
    private HttpServer server;
    private String today;
    String expectedResponse = "";

    @BeforeEach
    public void setup() throws IOException {
        taskParser = new JsonTaskParser();
        logger = new AppConsoleLogger();
        userService = new UserService(logger);
        taskRepository = new ListTaskRepository();
        today =  LocalDate.now().format(DateTimeFormatter.ISO_DATE).toString();
        taskService = new TaskService(userService, taskRepository, taskParser, logger);
        app = new ToDoApplication();
        server = app.createServer();
        server.start();
        createUser("janKowalski", "am!sK#123");
        createUser("pawel", "pawel");
    }

    @AfterEach
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    void createUser(String username, String password) throws IOException
    {
        URL url = new URL("http://localhost:8080/todo/user");
        HttpURLConnection conn1 = (HttpURLConnection) url.openConnection();
        conn1.setRequestMethod("POST");
        conn1.setDoOutput(true);
        conn1.setRequestProperty("Content-Type", "application/json");

        String requestBody1 = String.format("""
                {
                        "username": "%s",
                        "password": "%s"
                    }
                """, username, password);

        try (OutputStream os = conn1.getOutputStream()) {
            os.write(requestBody1.getBytes());
        }

        InputStream stream = conn1.getErrorStream() != null ? conn1.getErrorStream() : conn1.getInputStream();

        String response1 = new BufferedReader(new InputStreamReader(stream))
                .lines().collect(Collectors.joining());
    }

    String createTask(String username, String password, String description, String due,
                    int expectedStatusCode, String expectedResponse) throws IOException
    {
        URL url = new URL("http://localhost:8080/todo/task");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        String basicAuth = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        conn.setRequestProperty("Authorization", basicAuth);

        String requestBody1 = String.format("""
                {
                        "description": "%s",
                        "due": "%s"
                    }
                """, description, due);

        if(description == null)
        {
            requestBody1 = String.format("""
                {
                        "due": "%s"
                    }
                """, due);
        }
        else if(due == null)
        {
            requestBody1 = String.format("""
                {
                        "description": "%s"
                    }
                """, due);
        }

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody1.getBytes());
        }

        int status = conn.getResponseCode();
        assertEquals(expectedStatusCode, status);

        InputStream stream = conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream();

        String response = new BufferedReader(new InputStreamReader(stream))
                .lines().collect(Collectors.joining());

        assertTrue(response.contains(expectedResponse));

        return response;
    }

    String getTasks(String username, String password, int expectedStatusCode) throws IOException
    {
        URL url = new URL("http://localhost:8080/todo/task");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        String basicAuth = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        conn.setRequestProperty("Authorization", basicAuth);

        int status = conn.getResponseCode();
        assertEquals(expectedStatusCode, status);

        String response = new BufferedReader(new InputStreamReader(conn.getInputStream()))
                .lines().collect(Collectors.joining());

        return response;
    }

    String getTasks(String username, String password, int expectedStatusCode, String params) throws IOException
    {
        URL url = new URL("http://localhost:8080/todo/task" + "?" + params);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        String basicAuth = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        conn.setRequestProperty("Authorization", basicAuth);

        int status = conn.getResponseCode();
        assertEquals(expectedStatusCode, status);

        InputStream stream = conn.getErrorStream() != null
                ? conn.getErrorStream()
                : conn.getInputStream();

        String response = new BufferedReader(new InputStreamReader(stream))
                .lines().collect(Collectors.joining());

        return response;
    }

    String updateTask(String username, String password, String updatedBody, int expectedStatusCode, String params) throws IOException
    {
        URL url = new URL("http://localhost:8080/todo/task" + "?" + params);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        String basicAuth = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        conn.setRequestProperty("Authorization", basicAuth);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(updatedBody.getBytes());
        }

        int status = conn.getResponseCode();
        assertEquals(expectedStatusCode, status);

        InputStream stream = conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream();

        String response = new BufferedReader(new InputStreamReader(stream))
                .lines().collect(Collectors.joining());

        assertTrue(response.contains(expectedResponse));

        return response;
    }

    String deleteTask(String username, String password, int expectedStatusCode, String params) throws IOException
    {
        URL url = new URL("http://localhost:8080/todo/task" + "?" + params);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        String basicAuth = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        conn.setRequestProperty("Authorization", basicAuth);

        int status = conn.getResponseCode();
        assertEquals(expectedStatusCode, status);

        InputStream stream = conn.getErrorStream() != null
                ? conn.getErrorStream()
                : conn.getInputStream();

        String response = new BufferedReader(new InputStreamReader(stream))
                .lines().collect(Collectors.joining());

        return response;
    }

    @Test
    void givenValidAuth_whenCreatingTask_thenReturns201Created() throws IOException
    {
        expectedResponse = "New task added with id = ";
        createTask("janKowalski", "am!sK#123", "Kup mleko", today, 201, expectedResponse);
    }

    @Test
    void givenInvalidAuth_whenCreatingTask_thenReturns401Unauthorized() throws IOException {
        expectedResponse = "Invalid authentication";
        createTask("janKowalski", "", "Kup mleko", today, 401, expectedResponse);
    }


    @Test
    void givenMultipleTasks_whenGettingTasks_thenCorrectCountReturned() throws IOException {
        expectedResponse = "New task added with id = ";
        createTask("janKowalski", "am!sK#123", "Kup mleko#1", today, 201, expectedResponse);
        createTask("janKowalski", "am!sK#123", "Kup mleko#2", today, 201, expectedResponse);
        createTask("janKowalski", "am!sK#123", "Kup mleko#3", today, 201, expectedResponse);

        String response = getTasks("janKowalski", "am!sK#123", 200);
        int count = response.split("},\\{").length;
        assertEquals(3, count);
    }

    @Test
    void givenOtherUserTask_whenGetting_thenReturns403Forbidden() throws IOException {
        expectedResponse = "New task added with id = ";
        String task1 = createTask("janKowalski", "am!sK#123", "Kup mleko#1", today, 201, expectedResponse);
        String task2 = createTask("pawel", "pawel", "Kup mleko#1", today, 201, expectedResponse);

        String task_id = task1.replace("New task added with id =", "").trim();

        getTasks("pawel", "pawel", 403, "id=" + task_id);
    }

    @Test
    void givenInvalidTaskId_whenGetting_thenReturns404NotFound() throws IOException{
        expectedResponse = "Task not found\"";
        getTasks("pawel", "pawel", 404, "id=sometask");
    }

    @Test
    void givenValidTaskId_whenGetting_thenReturns200AndCorrectTask() throws IOException{
        String taskDescription = "Kup mleko#1";
        String task = createTask("pawel", "pawel", taskDescription, today, 201, expectedResponse);
        String task_id = task.replace("New task added with id =", "").trim();

        String response = getTasks("pawel", "pawel", 200, "id=" + task_id).toString();

        assertTrue(response.contains(task_id.substring(3)));
        assertTrue(response.contains(taskDescription));
        assertTrue(response.contains(today));
    }

    @Test
    void givenValidUpdate_whenUpdatingTask_thenTaskIsUpdated() throws IOException {
        String originalDescription = "Kup mleko#1";
        String task = createTask("pawel", "pawel", originalDescription, today, 201, expectedResponse);
        String task_id = task.replace("New task added with id =", "").trim();

        String newDescription = "New task updated";

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;
        LocalDate date = LocalDate.parse(today, formatter);
        LocalDate nextDay = date.plusDays(1);
        String newDate = nextDay.format(formatter);

        String blankDescriptionUpdate = String.format("""
        {
          "description": "%s",
          "due": "%s"
        }
        """, newDescription, newDate);

        String updateResponse = updateTask("pawel", "pawel", blankDescriptionUpdate, 200, "id=" + task_id);
        String getResponse = getTasks("pawel", "pawel", 200, "id=" + task_id);

        assertTrue(getResponse.contains(newDescription));
        assertTrue(getResponse.contains(newDate));
        assertFalse(getResponse.contains(originalDescription));
        assertFalse(getResponse.contains(today));

    }

    @Test
    void givenBlankFields_whenUpdatingTask_thenNoApplyChanges() throws IOException {
        String originalDescription = "Kup mleko#1";
        String task = createTask("pawel", "pawel", originalDescription, today, 201, expectedResponse);
        String task_id = task.replace("New task added with id =", "").trim();

        String newDescription = "";

        String blankDescriptionUpdate = String.format("""
        {
          "description": "%s"
        }
        """, newDescription);

        String updateResponse = updateTask("pawel", "pawel", blankDescriptionUpdate, 200, "id=" + task_id);
        String getResponse = getTasks("pawel", "pawel", 200, "id=" + task_id);

        assertTrue(getResponse.contains(originalDescription));
        assertFalse(getResponse.contains("\"description\":\"\""));
    }

    @Test
    void givenValidTask_whenDeleting_thenTaskIsRemoved() throws IOException {
        String originalDescription = "Kup mleko#1";
        String task = createTask("pawel", "pawel", originalDescription, today, 201, expectedResponse);
        String task_id = task.replace("New task added with id =", "").trim();

        deleteTask("pawel", "pawel", 200, "id=" + task_id);

        String response = getTasks("pawel", "pawel", 200);

        assertEquals("[]", response);
    }

    @Test
    void givenOtherUserTask_whenDeleting_thenReturns403Forbidden() throws IOException {
        String task1 = createTask("janKowalski", "am!sK#123", "Kup mleko#1", today, 201, expectedResponse);
        String task2 = createTask("pawel", "pawel", "Kup mleko#1", today, 201, expectedResponse);

        String task_id = task1.replace("New task added with id =", "").trim();

        deleteTask("pawel", "pawel", 403, "id=" + task_id);
    }

}
