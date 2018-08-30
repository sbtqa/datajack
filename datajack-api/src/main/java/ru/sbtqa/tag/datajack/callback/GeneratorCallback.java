package ru.sbtqa.tag.datajack.callback;

public interface GeneratorCallback extends Callback<CallbackData, Object> {

    /**
     * Callback implementation. Do nor forget to check cache before generating
     * value
     *
     * @param callbackData callback data
     * @return Callback instance
     */
    @Override
    Object call(CallbackData callbackData);

}
