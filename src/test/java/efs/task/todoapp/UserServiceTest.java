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
public class UserServiceTest {

    private UserService userService;
    private AppLogger logger;
    private ToDoApplication app;
    private HttpServer server;
    private String today;

    @BeforeEach
    public void setup() throws IOException {
        logger = new AppConsoleLogger();
        userService = new UserService(logger);
        today =  LocalDate.now().format(DateTimeFormatter.ISO_DATE).toString();
        app = new ToDoApplication();
        server = app.createServer();
        server.start();
    }

    @AfterEach
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    void createUser(String username, String password, int expectedStatusCode, String expectedResponse) throws IOException
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

        if(username == null)
        {
            requestBody1 = String.format("""
                {
                        "password": "%s"
                    }
                """, password);
        }
        else if(password == null)
        {
            requestBody1 = String.format("""
                {
                        "username": "%s"
                    }
                """, username);
        }

        try (OutputStream os = conn1.getOutputStream()) {
            os.write(requestBody1.getBytes());
        }

        int status1 = conn1.getResponseCode();
        assertEquals(expectedStatusCode, status1);

        InputStream stream = conn1.getErrorStream() != null ? conn1.getErrorStream() : conn1.getInputStream();

        String response1 = new BufferedReader(new InputStreamReader(stream))
                .lines().collect(Collectors.joining());

        assertTrue(response1.contains(expectedResponse));
    }

    @Test
    void whenCreatingTwoValidUsers_thenReturns201() throws IOException {
        String expectedResponse = "User created: janKowalski";
        createUser("janKowalski", "am!sK#123", 201, expectedResponse);

        expectedResponse = "User created: pawel";
        createUser("pawel", "pawel", 201, expectedResponse);
    }

    @Test
    void whenCreatingUserWithDuplicateUsername_thenReturns409() throws IOException {
        String expectedResponse = "User created: janKowalski";
        createUser("janKowalski", "am!sK#123", 201, expectedResponse);

        expectedResponse = "User already exists";
        createUser("janKowalski", "am!sK#123", 409, expectedResponse);
    }

    @Test
    void shouldReturn400_whenUserDataIsMissingOrBlank() throws IOException {
        String expectedResponse = "Missing username or password";
        createUser("janKowalski", "", 400, expectedResponse);
    }
}
