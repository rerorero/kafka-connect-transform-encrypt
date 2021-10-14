package io.github.rerorero.kafka.connect.transform.encrypt.exception;

public class ClientErrorException extends ServiceException{
    public ClientErrorException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientErrorException(String message) {
        super(message);
    }

    public ClientErrorException(Throwable cause) {
        super(cause);
    }

    public ClientErrorException() {
        super();
    }
}
