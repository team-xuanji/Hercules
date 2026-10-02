package team.magic.flute.hercules.manager.exception;

public class InternalError extends RuntimeException {
    public InternalError(String message) {
        super(message);
    }
    public InternalError(String message,Exception e) {
        super(message,e);
    }
}
