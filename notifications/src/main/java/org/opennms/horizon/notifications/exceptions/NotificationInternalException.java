package org.opennms.horizon.notifications.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
public class NotificationInternalException extends NotificationException {
    public NotificationInternalException() {
    }

    public NotificationInternalException(String message) {
        super(message);
    }

    public NotificationInternalException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotificationInternalException(Throwable cause) {
        super(cause);
    }
}
