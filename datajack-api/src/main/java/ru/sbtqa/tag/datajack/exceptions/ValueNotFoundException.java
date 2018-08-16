package ru.sbtqa.tag.datajack.exceptions;

public class ValueNotFoundException extends DataException {

    public ValueNotFoundException(String message) {
        super(message);
    }

    public ValueNotFoundException(String string, Throwable ex) {
        super(string, ex);
    }
}
