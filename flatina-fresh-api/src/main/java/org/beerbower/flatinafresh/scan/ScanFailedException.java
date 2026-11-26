package org.beerbower.flatinafresh.scan;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class ScanFailedException extends RuntimeException {
    public ScanFailedException(String message) {
        super(message);
    }

    public ScanFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
