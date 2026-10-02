package team.magic.flute.hercules.manager.exception;

public class PluginRegisterFailedException extends RuntimeException {
    public PluginRegisterFailedException(String message) {
        super(message);
    }
    public PluginRegisterFailedException(String message,Exception e) {
        super(message,e);
    }
}
