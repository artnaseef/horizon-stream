package org.opennms.horizon.notifications.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
public class NotificationAPIException extends NotificationException {
    public NotificationAPIException() {
    }

    public NotificationAPIException(String message) {
        super(message);
    }

    public NotificationAPIException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotificationAPIException(Throwable cause) {
        super(cause);
    }
}
