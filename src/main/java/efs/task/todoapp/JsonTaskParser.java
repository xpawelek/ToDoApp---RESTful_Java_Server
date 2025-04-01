package efs.task.todoapp;

import java.util.List;
import com.google.gson.Gson;

public class JsonTaskParser implements TaskParser {
    private Gson gson;

    public JsonTaskParser() {
        gson = new Gson();
    }

    @Override
    public Task parseTask(String task) {
        return gson.fromJson(task, Task.class);
    }

    @Override
    public String serializeTask(Task task) {
        return gson.toJson(task);
    }

    @Override
    public String serializeTask(List<Task> tasks) {
        return gson.toJson(tasks);
    }
}
