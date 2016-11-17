package ru.sbtqa.tag.datajack.exceptions;

/**
 *
 * @author Viktor Sidochenko <viktor.sidochenko@gmail.com>
 */
public class FileNotFoundException extends DataException {

    public FileNotFoundException(String message) {
        super(message);
    }

    public FileNotFoundException(String string, Throwable ex) {
        super(string, ex);
    }

}
