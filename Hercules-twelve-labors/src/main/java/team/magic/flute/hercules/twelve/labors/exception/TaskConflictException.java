package team.magic.flute.hercules.twelve.labors.exception;

/**
 * Task Conflict Exception for Hercules Business Access Gateway
 *
 * <p>Runtime exception for task execution conflicts within the Hercules business access gateway.
 * Thrown when task operations conflict with existing tasks or system constraints.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
public class TaskConflictException extends RuntimeException {
    public TaskConflictException(String message) {
        super(message);
    }
    public TaskConflictException(String message,Exception e) {
        super(message,e);
    }
}
