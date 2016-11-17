package ru.sbtqa.tag.datajack.exceptions;

/**
 *
 * @author Viktor Sidochenko <viktor.sidochenko@gmail.com>
 */
public class DataException extends Exception {

    public DataException(String message) {
        super(message);
    }

    public DataException(String message, Throwable t) {
        super(message, t);
    }
}
