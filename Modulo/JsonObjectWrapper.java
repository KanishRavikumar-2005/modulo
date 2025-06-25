package Modulo;

import java.util.Map;

public class JsonObjectWrapper {
    private final Map<String, String> data;

    public JsonObjectWrapper(Map<String, String> data) {
        this.data = data;
    }

    public String get(String key) {
        return data.getOrDefault(key, null);
    }

    @Override
    public String toString() {
        return data.toString();
    }
}
