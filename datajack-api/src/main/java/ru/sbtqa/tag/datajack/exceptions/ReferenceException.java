package ru.sbtqa.tag.datajack.exceptions;

public class ReferenceException extends DataException {

    public ReferenceException(String message) {
        super(message);
    }

    public ReferenceException(String string, Throwable ex) {
        super(string, ex);
    }
}
