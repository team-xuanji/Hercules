package team.magic.flute.hercules.manager.exception;


import team.magic.flute.hercules.common.util.StrFormat;

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
