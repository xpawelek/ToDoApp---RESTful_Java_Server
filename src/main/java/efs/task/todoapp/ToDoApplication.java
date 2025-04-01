package efs.task.todoapp;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;

public class ToDoApplication {

    public ToDoApplication() {};

    public HttpServer createServer() throws IOException
    {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        ListTaskRepository repository = new ListTaskRepository();
        JsonTaskParser taskParser = new JsonTaskParser();
        AppConsoleLogger logger = new AppConsoleLogger();
        UserService userService = new UserService(logger);
        TaskService taskService = new TaskService(userService, repository, taskParser, logger);
        server.createContext("/todo/user", userService);
        server.createContext("/todo/task", taskService);
        server.createContext("/", new NotFoundHandler());
        server.setExecutor(null);
        return server;
    }
}
