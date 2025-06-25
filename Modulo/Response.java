package Modulo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Response {
    private final int code;
    private final String body;
    private final List<Map<String, String>> rows = new ArrayList<>();

    public Response(int code, String body) {
        this.code = code;
        this.body = body;
        parse(); // fill `rows`
    }

    public int code() {
        return code;
    }

    public String body() {
        return body;
    }

    public int size() {
        return rows.size();
    }

    public JsonObjectWrapper get(int index) {
        if (index >= 0 && index < rows.size()) {
            return new JsonObjectWrapper(rows.get(index));
        }
        return null;
    }

    private void parse() {
        String json = body.trim();
        if (!json.startsWith("[") || !json.endsWith("]")) return;
        json = json.substring(1, json.length() - 1).trim(); // strip [ ]

        // crude way to split multiple objects — only supports flat fields!
        String[] objects = json.split("\\},\\s*\\{");
        for (int i = 0; i < objects.length; i++) {
            String obj = objects[i].trim();
            if (!obj.startsWith("{")) obj = "{" + obj;
            if (!obj.endsWith("}")) obj = obj + "}";
            rows.add(parseObject(obj));
        }
    }

    private Map<String, String> parseObject(String json) {
        Map<String, String> map = new HashMap<>();
        json = json.substring(1, json.length() - 1); // remove { }

        String[] pairs = json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replaceAll("^\"|\"$", "");
                String val = kv[1].trim().replaceAll("^\"|\"$", "");
                map.put(key, val);
            }
        }
        return map;
    }

    public String outData() {
        return body;
    }
    @Override
    public String toString() {
        return "Status Code: " + code + "\nBody: " + body;
    }
}
