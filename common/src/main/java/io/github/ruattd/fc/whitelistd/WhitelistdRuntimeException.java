package io.github.ruattd.fc.whitelistd;

public class WhitelistdRuntimeException extends RuntimeException {
    public WhitelistdRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public WhitelistdRuntimeException(String message) {
        super(message);
    }
}
