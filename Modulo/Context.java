package Modulo;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class Context {
    private static final ThreadLocal<HttpExchange> threadLocal = new ThreadLocal<>();
    private static final Map<String, String> formInputs = new HashMap<>();
    private static final Map<String, Map<String, String>> sessions = new HashMap<>();

    private static String sessionId = null;

    public static void load(HttpExchange exchange) {
        threadLocal.set(exchange);
        formInputs.clear();
        sessionId = null;

        // --- Parse GET parameters ---
        String query = exchange.getRequestURI().getQuery();
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                    String val = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                    formInputs.put(key, val);
                } else if (kv.length == 1) {
                    String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                    formInputs.put(key, "");
                }
            }
        }

        // --- Parse POST form ---
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            try (InputStream is = exchange.getRequestBody()) {
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                String[] pairs = body.split("&");
                for (String pair : pairs) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2) {
                        String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                        String val = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                        formInputs.put(key, val);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // --- Ensure session is initialized ---
        getOrCreateSessionId();
    }

    public static HttpExchange req() {
        return threadLocal.get();
    }

    public static String input(String key) {
        return formInputs.getOrDefault(key, "");
    }

    public static boolean method(String m) {
        return m.equalsIgnoreCase(req().getRequestMethod());
    }

    public static Map<String, String> params() {
        return formInputs;
    }

    public static ParamMapWithDefault params(String defaultValue) {
        return new ParamMapWithDefault(formInputs, defaultValue);
    }

    public static class ParamMapWithDefault {
        private final Map<String, String> map;
        private final String fallback;

        public ParamMapWithDefault(Map<String, String> map, String fallback) {
            this.map = map;
            this.fallback = fallback;
        }

        public String get(String key) {
            return map.getOrDefault(key, fallback);
        }
    }

    public static void redirect(String location) {
        try {
            HttpExchange exchange = req();
            exchange.getResponseHeaders().add("Location", location);
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        } catch (IOException e) {
            throw new RuntimeException("Redirect failed: " + e.getMessage());
        }
    }

    // ===== 🍪 Cookies =====

    public static void setCookie(String name, String value, int maxAgeSeconds) {
        String cookie = name + "=" + value + "; Path=/; HttpOnly";
        if (maxAgeSeconds > 0) {
            cookie += "; Max-Age=" + maxAgeSeconds;
        }
        req().getResponseHeaders().add("Set-Cookie", cookie);
    }

    public static Map<String, String> getCookies() {
        List<String> cookieHeaders = req().getRequestHeaders().get("Cookie");
        Map<String, String> cookies = new HashMap<>();

        if (cookieHeaders != null) {
            for (String header : cookieHeaders) {
                String[] pairs = header.split(";");
                for (String pair : pairs) {
                    String[] kv = pair.trim().split("=", 2);
                    if (kv.length == 2) {
                        cookies.put(kv[0], kv[1]);
                    }
                }
            }
        }

        return cookies;
    }

    // ===== 💾 Sessions =====

    private static String getOrCreateSessionId() {
        if (sessionId != null) return sessionId;

        Map<String, String> cookies = getCookies();
        sessionId = cookies.get("SESSION_ID");

        if (sessionId == null || !sessions.containsKey(sessionId)) {
            sessionId = UUID.randomUUID().toString();
            sessions.put(sessionId, new HashMap<>());
            setCookie("SESSION_ID", sessionId, 86400); // 1 day
        }

        return sessionId;
    }

    public static Map<String, String> session() {
        return sessions.get(getOrCreateSessionId());
    }

    // ===== 🧾 Logging =====

    public static void logAll() {
        HttpExchange exchange = req();
        System.out.println("---- Context Log ----");
        System.out.println("Method: " + exchange.getRequestMethod());
        System.out.println("Path: " + exchange.getRequestURI().getPath());
        System.out.println("Headers:");
        for (Map.Entry<String, List<String>> entry : exchange.getRequestHeaders().entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + String.join(", ", entry.getValue()));
        }
        System.out.println("Form Inputs:");
        for (Map.Entry<String, String> entry : formInputs.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
        System.out.println("----------------------");
    }

    public static void logAll(String filename) {
        try {
            Files.writeString(Paths.get(filename),
                "Session ID: " + getOrCreateSessionId() + "\n" +
                session().toString() + "\n\n",
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Log write failed: " + e.getMessage());
        }
    }

    public static void logLine() {
        HttpExchange exchange = req();
        String time = java.time.LocalDateTime.now().toString();
        String ip = exchange.getRemoteAddress().getAddress().getHostAddress();
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();
        String inputs = formInputs.isEmpty() ? "-" : formInputs.toString().replaceAll("[\\n\\r]+", " ");

        System.out.printf("[%s] %s %s %s%s | Form: %s%n",
            time, ip, method, path,
            (query != null ? "?" + query : ""), inputs
        );
    }

    public static void logLine(String filename) {
        String line = String.format("[%s] %s | %s%n",
            java.time.LocalDateTime.now(),
            getOrCreateSessionId(),
            session().toString()
        );

        try {
            Files.writeString(Paths.get(filename), line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Failed to write log: " + e.getMessage());
        }
    }

    public static String charId(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        Random random = new Random();
        
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }

        return sb.toString();
    }
}
