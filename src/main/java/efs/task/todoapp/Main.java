package efs.task.todoapp;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        ToDoApplication app = new ToDoApplication();
        HttpServer server = app.createServer();
        server.start();
        System.out.println("Server started on http://localhost:8080");
    }
}