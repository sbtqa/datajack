package ru.sbtqa.tag.datajack.exceptions;

public class StashKeyNotFoundException extends RuntimeException {

    public StashKeyNotFoundException(String message) {
        super(message);
    }

    public StashKeyNotFoundException(String string, Throwable ex) {
        super(string, ex);
    }
}
