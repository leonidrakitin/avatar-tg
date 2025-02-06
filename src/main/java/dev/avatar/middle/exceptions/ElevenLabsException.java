package dev.avatar.middle.exceptions;

import dev.avatar.middle.exceptions.enums.ElevenLabsErrorCode;
import dev.avatar.middle.exceptions.enums.HeyGenErrorCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class ElevenLabsException extends RuntimeException {

    private final ElevenLabsErrorCode errorCode;

    public ElevenLabsException(ElevenLabsErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        log.error("HeyGenException: {} - {}", errorCode, message);
    }

    public ElevenLabsException(ElevenLabsErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        log.error("HeyGenException: {} - {}", errorCode, message, cause);
    }
}
