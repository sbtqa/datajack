package ru.sbtqa.tag.datajack.exceptions;

/**
 *
 * @author Viktor Sidochenko <viktor.sidochenko@gmail.com>
 */
public class DataParseException extends DataException {

    public DataParseException(String message) {
        super(message);
    }

    public DataParseException(String string, Throwable ex) {
        super(string, ex);
    }

}
