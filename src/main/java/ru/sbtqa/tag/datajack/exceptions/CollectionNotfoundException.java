package ru.sbtqa.tag.datajack.exceptions;

/**
 *
 * @author Viktor Sidochenko <viktor.sidochenko@gmail.com>
 */
public class CollectionNotfoundException extends DataException {

    public CollectionNotfoundException(String message) {
        super(message);
    }

    public CollectionNotfoundException(String string, Throwable ex) {
        super(string, ex);
    }

}
