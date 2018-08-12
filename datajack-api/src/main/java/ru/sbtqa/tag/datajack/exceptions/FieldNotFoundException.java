package ru.sbtqa.tag.datajack.exceptions;

public class FieldNotFoundException extends DataException {

    public FieldNotFoundException(String message) {
        super(message);
    }

    public FieldNotFoundException(String string, Throwable ex) {
        super(string, ex);
    }
}
