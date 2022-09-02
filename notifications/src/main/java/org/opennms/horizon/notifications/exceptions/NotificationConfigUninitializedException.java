package org.opennms.horizon.notifications.exceptions;

public class NotificationConfigUninitializedException extends NotificationException{
    public NotificationConfigUninitializedException() {
    }

    public NotificationConfigUninitializedException(String message) {
        super(message);
    }

    public NotificationConfigUninitializedException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotificationConfigUninitializedException(Throwable cause) {
        super(cause);
    }
}
