package io.github.rerorero.kafka.connect.transform.encrypt.exception;

public class ServerErrorException extends ServiceException {
    public ServerErrorException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServerErrorException(String message) {
        super(message);
    }

    public ServerErrorException(Throwable cause) {
        super(cause);
    }

    public ServerErrorException() {
        super();
    }
}
