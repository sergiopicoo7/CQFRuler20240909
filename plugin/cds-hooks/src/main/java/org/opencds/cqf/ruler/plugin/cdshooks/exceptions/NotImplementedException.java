package org.opencds.cqf.ruler.plugin.cdshooks.exceptions;

@SuppressWarnings("serial")
public class NotImplementedException extends RuntimeException {
    public NotImplementedException() {}

    public NotImplementedException(String message) {
        super(message);
    }
}
