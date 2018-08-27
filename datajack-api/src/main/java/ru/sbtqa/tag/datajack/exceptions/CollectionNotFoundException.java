package ru.sbtqa.tag.datajack.exceptions;

public class CollectionNotFoundException extends DataException {

    public CollectionNotFoundException(String message) {
        super(message);
    }

    public CollectionNotFoundException(String string, Throwable ex) {
        super(string, ex);
    }

}
