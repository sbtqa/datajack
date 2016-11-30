package ru.sbtqa.tag.datajack.exceptions;

public class FileNotFoundException extends DataException {

    public FileNotFoundException(String message) {
        super(message);
    }

    public FileNotFoundException(String string, Throwable ex) {
        super(string, ex);
    }

}
