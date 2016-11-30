package ru.sbtqa.tag.datajack.callback;

import javafx.util.Callback;

public interface GeneratorCallback extends Callback<CallbackData, Object> {

    /**
     * Callback implementation. Do nor forget to check cache before generating
     * value
     *
     * @param callbackData callback data
     * @return TODO
     */
    @Override
    public Object call(CallbackData callbackData);

}
