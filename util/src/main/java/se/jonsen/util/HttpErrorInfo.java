package se.jonsen.util;

import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;

public class HttpErrorInfo {
    private final ZonedDateTime timestamp;
    private final String path;
    private final HttpStatus httpStatus;
    private final String message;

    private HttpErrorInfo() {
        this.timestamp = null;
        this.path = null;
        this.httpStatus = null;
        this.message = null;
    }

    public HttpErrorInfo(String path, HttpStatus httpStatus, String message) {
        this.timestamp = ZonedDateTime.now();
        this.path = path;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public String getError() {
        return httpStatus.getReasonPhrase();
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public String getPath() {
        return path;
    }

    public String getMessage() {
        return message;
    }

    int getStatus() {
        return httpStatus.value();
    }
}
