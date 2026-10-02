package team.magic.flute.hercules.twelve.labors.exception;


import team.magic.flute.hercules.common.util.StrFormat;

/**
 * Business Exception for Hercules Business Access Gateway
 *
 * <p>Runtime exception for business logic errors within the Hercules business access gateway.
 * Supports formatted error messages and cause chaining for comprehensive error reporting.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
public class BusinessException extends RuntimeException {

    public BusinessException(Throwable cause, String msgFormat, Object... args) {
        super(StrFormat.format(msgFormat, args), cause);
    }

    public BusinessException(String msgFormat, Object... args) {
        super(StrFormat.format(msgFormat, args));
    }

    public BusinessException(Throwable cause) {
        super(cause);
    }
}
