package ru.sbtqa.tag.datajack.exceptions;

/**
 *
 * @author Viktor Sidochenko <viktor.sidochenko@gmail.com>
 */
public class FieldNotFoundException extends DataException {

    public FieldNotFoundException(String message) {
        super(message);
    }

    public FieldNotFoundException(String string, Throwable ex) {
        super(string, ex);
    }
}
