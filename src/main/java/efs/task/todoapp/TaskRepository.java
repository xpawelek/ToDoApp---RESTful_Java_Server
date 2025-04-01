package efs.task.todoapp;

import java.util.Set;

public interface TaskRepository {
    Set<Task> loadTasks();
    void saveTasks(Set<Task> tasks);
}
