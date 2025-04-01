package efs.task.todoapp;

import java.util.List;

public interface TaskParser {
    Task parseTask(String task); //typ -> string
    String serializeTask(Task task); //string -> typ
    String serializeTask(List<Task> tasks); //string -> typ
}
