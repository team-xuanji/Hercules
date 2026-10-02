package team.magic.flute.hercules.manager.exception;

public class TaskConflictException extends RuntimeException {
    public TaskConflictException(String message) {
        super(message);
    }
    public TaskConflictException(String message,Exception e) {
        super(message,e);
    }
}
