package team.magic.flute.hercules.twelve.labors.exception;

/**
 * Internal Error Exception for Hercules Business Access Gateway
 *
 * <p>Runtime exception for internal system errors within the Hercules business access gateway.
 * Used for infrastructure and system-level failures that require immediate attention.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
public class InternalError extends RuntimeException {
    public InternalError(String message) {
        super(message);
    }
    public InternalError(String message,Exception e) {
        super(message,e);
    }
}
