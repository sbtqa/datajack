package ru.sbtqa.tag.datajack.exceptions;

/**
 *
 * @author Viktor Sidochenko <viktor.sidochenko@gmail.com>
 */
public class ReferenceException extends DataException {

    public ReferenceException(String message) {
        super(message);
    }

    public ReferenceException(String string, Throwable ex) {
        super(string, ex);
    }
}
