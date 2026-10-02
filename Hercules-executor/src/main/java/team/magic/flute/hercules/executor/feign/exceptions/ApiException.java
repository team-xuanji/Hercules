package team.magic.flute.hercules.executor.feign.exceptions;


public class ApiException  extends RuntimeException{

    final static String DEFAULT_MSG = "Oops, something wrong.";

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
