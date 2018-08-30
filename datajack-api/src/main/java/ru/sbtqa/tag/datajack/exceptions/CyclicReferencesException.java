package ru.sbtqa.tag.datajack.exceptions;

public class CyclicReferencesException extends DataException {

    public CyclicReferencesException(String message) {
        super(message);
    }

    public CyclicReferencesException(String string, Throwable ex) {
        super(string, ex);
    }

}
