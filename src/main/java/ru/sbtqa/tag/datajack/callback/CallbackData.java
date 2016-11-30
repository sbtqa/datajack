package ru.sbtqa.tag.datajack.callback;

public class CallbackData {

    private final String result;
    private final String path;

    /**
     * Crate callback data
     *
     * @param path use path as key to store cache if needed
     * @param result string to generate by
     */
    public CallbackData(String path, String result) {
        this.path = path;
        this.result = result;
    }

    /**
     * @return Get value path
     */
    public String getPath() {
        return path;
    }

    /**
     * @return value with data to be generated (or got from cache if already
     * generated
     */
    public String getResult() {
        return result;
    }
}
