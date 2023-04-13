package org.technologybrewery.krausening;

public class KrauseningException extends RuntimeException {

    private static final long serialVersionUID = -4627264334495086094L;

    public KrauseningException() {
        super();
    }

    public KrauseningException(String message, Throwable cause) {
        super(message, cause);
    }

    public KrauseningException(String message) {
        super(message);
    }

    public KrauseningException(Throwable cause) {
        super(cause);
    }

}