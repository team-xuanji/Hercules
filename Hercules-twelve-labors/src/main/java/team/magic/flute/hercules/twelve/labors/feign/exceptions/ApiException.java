package team.magic.flute.hercules.twelve.labors.feign.exceptions;


/**
 * API Exception for Business Access Gateway
 *
 * <p>Runtime exception for API communication errors within the Hercules
 * business access gateway Feign client integration.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
public class ApiException  extends RuntimeException{

    final static String DEFAULT_MSG = "An unknown exception occurred, please contact the administrator.";

    private FailResult failResult;

    public ApiException() {
        super(DEFAULT_MSG);
    }

    public ApiException(final String message) {
        super(message);
    }

    public ApiException(final String message , FailResult failResult) {
        super(message);
        this.failResult = failResult;
    }

    public ApiException(FailResult failResult,String message, Throwable cause)
    {
        super(message, cause);
        this.failResult = failResult ;
    }

    public FailResult getFailResult() {
        return failResult;
    }

}
