package efs.task.todoapp;

import lombok.Getter;
import lombok.Setter;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.time.LocalDate;

@Getter
@Setter
public class Task {
    private final String id;
    private String description;
    private String due;
    private transient String owner;
    private TaskStatus status;

    public Task()
    {
        this.id = UUID.randomUUID().toString();
        this.status = TaskStatus.TODO;
    }

    public Task(String description, String due, String owner) {
        this.id = UUID.randomUUID().toString();
        this.description = description;
        LocalDate parsed = LocalDate.parse(due);
        this.due = parsed.format(DateTimeFormatter.ISO_DATE);
        this.owner = owner;
        this.status = TaskStatus.TODO;
    }

    public void setDue(String due)
    {
        LocalDate parsed = LocalDate.parse(due);
        this.due = parsed.format(DateTimeFormatter.ISO_DATE);
    }
}

