package ru.sbtqa.tag.datajack.callback;

/**
 * Simple callback to be injected into provider
 *
 * @param <P> Object to inject
 * @param <R> Object to return
 */
public interface Callback<P, R> {

    /**
     * @param p Object to inject
     * @return callback result
     */
    R call(P p);
}
