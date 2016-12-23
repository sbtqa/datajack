package ru.sbtqa.tag.datajack.exceptions;

public class GeneratorException extends DataException {

    public GeneratorException(String message) {
        super(message);
    }

    public GeneratorException(String string, Throwable ex) {
        super(string, ex);
    }
}
