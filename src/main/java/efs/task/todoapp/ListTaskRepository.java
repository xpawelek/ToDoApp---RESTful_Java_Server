package efs.task.todoapp;

import java.util.HashSet;
import java.util.Set;

public class ListTaskRepository implements TaskRepository {
    private Set<Task> tasks;

    public ListTaskRepository() {
        tasks = new HashSet<>();
    }

    @Override
    public Set<Task> loadTasks() {
        return tasks;
    }

    @Override
    public void saveTasks(Set<Task> tasks) {
        this.tasks = tasks;
    }
}
