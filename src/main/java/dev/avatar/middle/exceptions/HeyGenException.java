package dev.avatar.middle.exceptions;

import dev.avatar.middle.exceptions.enums.HeyGenErrorCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class HeyGenException extends RuntimeException {

    private final HeyGenErrorCode errorCode;

    public HeyGenException(HeyGenErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        log.error("HeyGenException: {} - {}", errorCode, message);
    }

    public HeyGenException(HeyGenErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        log.error("HeyGenException: {} - {}", errorCode, message, cause);
    }
}
