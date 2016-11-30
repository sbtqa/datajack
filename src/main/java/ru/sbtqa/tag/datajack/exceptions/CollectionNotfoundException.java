package ru.sbtqa.tag.datajack.exceptions;

public class CollectionNotfoundException extends DataException {

    public CollectionNotfoundException(String message) {
        super(message);
    }

    public CollectionNotfoundException(String string, Throwable ex) {
        super(string, ex);
    }

}
