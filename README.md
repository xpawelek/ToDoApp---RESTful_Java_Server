# ToDo Application – RESTful Java Server

A task manager with user authentication, created using raw Java + HTTP server.

## Features

- User registration & authentication (Basic Auth)
- Task creation, reading, updating, and deletion
- Tasks include description, due date, status (TODO/DONE/OVERDUE)
- Validation and error handling
- Full JUnit 5 test coverage
- JSON parsing via Gson
- Logging via SLF4J + Logback

##  How to run

```bash
mvn clean install
java -jar target/ToDoApplication-1.0-SNAPSHOT-jar-with-dependencies.jar
