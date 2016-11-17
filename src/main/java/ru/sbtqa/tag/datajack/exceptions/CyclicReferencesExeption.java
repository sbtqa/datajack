package ru.sbtqa.tag.datajack.exceptions;

/**
 *
 * @author Viktor Sidochenko <viktor.sidochenko@gmail.com>
 */
public class CyclicReferencesExeption extends DataException {

    public CyclicReferencesExeption(String message) {
        super(message);
    }

    public CyclicReferencesExeption(String string, Throwable ex) {
        super(string, ex);
    }

}
