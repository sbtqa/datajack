package ru.sbtqa.tag.datajack.callback;

import javafx.util.Callback;

/**
 *
 * @author Viktor Sidochenko <viktor.sidochenko@gmail.com>
 */
public interface GeneratorCallback extends Callback<CallbackData, Object> {

    /**
     * Callback implementation. Do nor forget to check cache before generating
     * value
     *
     * @param callbackData callback data
     * @return
     */
    @Override
    public Object call(CallbackData callbackData);

}
