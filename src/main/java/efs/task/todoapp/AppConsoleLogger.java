package efs.task.todoapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppConsoleLogger implements AppLogger {
    private static final Logger logger = LoggerFactory.getLogger(AppConsoleLogger.class);

    public void log(String message)  {
        logger.info(message);
    }
}