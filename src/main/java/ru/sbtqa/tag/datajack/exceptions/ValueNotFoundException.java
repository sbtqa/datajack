package ru.sbtqa.tag.datajack.exceptions;

/**
 *
 * @author Viktor Sidochenko <viktor.sidochenko@gmail.com>
 */
public class ValueNotFoundException extends DataException {

    public ValueNotFoundException(String message) {
        super(message);
    }

    public ValueNotFoundException(String string, Throwable ex) {
        super(string, ex);
    }
}
