package org.opennms.horizon.notifications.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class NotificationBadDataException extends NotificationException {
    public NotificationBadDataException() {
    }

    public NotificationBadDataException(String message) {
        super(message);
    }

    public NotificationBadDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotificationBadDataException(Throwable cause) {
        super(cause);
    }
}
