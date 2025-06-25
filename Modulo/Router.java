package Modulo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class Router {
    public interface RouteHandler {
        String handle(HttpExchange exchange, Map<String, String> params);
    }

    static class Route {
        Pattern pattern;
        String[] paramNames;
        RouteHandler handler;
        String[] methods;

        Route(Pattern pattern, String[] paramNames, RouteHandler handler, String[] methods) {
            this.pattern = pattern;
            this.paramNames = paramNames;
            this.handler = handler;
            this.methods = methods;
        }
    }

    private final List<Route> routes = new ArrayList<>();

    @SuppressWarnings("CollectionsToArray")
    public void add(String route, RouteHandler handler, String[] methods) {
        List<String> paramNames = new ArrayList<>();
        Matcher m = Pattern.compile(":([a-zA-Z][a-zA-Z0-9_]*)").matcher(route);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            paramNames.add(m.group(1));
            m.appendReplacement(sb, "([^/]+)");
        }
        m.appendTail(sb);
        Pattern pattern = Pattern.compile("^" + sb + "$");
        routes.add(new Route(pattern, paramNames.toArray(new String[0]), handler, methods));
    }

    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        for (Route route : routes) {
            Matcher matcher = route.pattern.matcher(path);
            if (matcher.matches() && Arrays.asList(route.methods).contains(method)) {
                Map<String, String> params = new HashMap<>();
                for (int i = 0; i < matcher.groupCount(); i++) {
                    params.put(route.paramNames[i], matcher.group(i + 1));
                }

                Context.load(exchange);
                String response = route.handler.handle(exchange, params);
                send(exchange, 200, response);
                return;
            }
        }

        send(exchange, 404, "<h1>404 Not Found</h1>");
    }

    public void handleContext(HttpExchange exchange) throws IOException {
    String rawPath = exchange.getRequestURI().getPath();
    String contextPath = exchange.getHttpContext().getPath(); // e.g., "/api"
    String path = rawPath.substring(contextPath.length());    // remove "/api"

    if (path.isEmpty()) path = "/";

    String method = exchange.getRequestMethod();

    for (Route route : routes) {
        Matcher matcher = route.pattern.matcher(path);
        if (matcher.matches() && Arrays.asList(route.methods).contains(method)) {
            Map<String, String> params = new HashMap<>();
            for (int i = 0; i < matcher.groupCount(); i++) {
                params.put(route.paramNames[i], matcher.group(i + 1));
            }

            Context.load(exchange);
            String response = route.handler.handle(exchange, params);
            send(exchange, 200, response);
            return;
        }
    }

    send(exchange, 404, "<h1>404 Not Found</h1>");
}


    private void send(HttpExchange exchange, int status, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/html");
        exchange.sendResponseHeaders(status, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    // ✅ Static file serving
  public void serveStatic(String folderPath) {
    Path basePath = Paths.get(folderPath).toAbsolutePath().normalize();

    this.add(".*", (exchange, params) -> {
        String requestPath = exchange.getRequestURI().getPath();
        if (requestPath.equals("/")) return null;

        Path requestedFile = basePath.resolve("." + requestPath).normalize();

        // Prevent path traversal
        if (!requestedFile.startsWith(basePath)) {
            sendSafely(exchange, 403, "403 Forbidden");
            return "";
        }

        if (!Files.exists(requestedFile) || Files.isDirectory(requestedFile)) {
            return null; // Let other routes try or send 404
        }

        try {
            String mime = Files.probeContentType(requestedFile);
            if (mime == null) mime = "application/octet-stream";

            byte[] bytes = Files.readAllBytes(requestedFile);

            exchange.getResponseHeaders().set("Content-Type", mime);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (IOException e) {
            sendSafely(exchange, 500, "500 Internal Server Error");
        }

        return "";
    }, new String[] { "GET" });
}

public void serveStatic(String folderPath, String routePrefix) {
    Path basePath = Paths.get(folderPath).toAbsolutePath().normalize();
    String cleanPrefix = routePrefix.endsWith("/") ? routePrefix : routePrefix + "/";

    // // Escape dashes and dots in prefix for regex
    // String escapedPrefix = Pattern.quote(cleanPrefix);

    // Match only paths under the prefix
    this.add(cleanPrefix + ".*", (exchange, params) -> {
        String requestPath = exchange.getRequestURI().getPath();

        if (!requestPath.startsWith(cleanPrefix)) return null;

        String relativePath = requestPath.substring(cleanPrefix.length());
        Path requestedFile = basePath.resolve(relativePath).normalize();

        // Prevent directory traversal
        if (!requestedFile.startsWith(basePath)) {
            sendSafely(exchange, 403, "403 Forbidden");
            return "";
        }

        if (!Files.exists(requestedFile) || Files.isDirectory(requestedFile)) {
            return null;
        }

        try {
            String mime = Files.probeContentType(requestedFile);
            if (mime == null) mime = "application/octet-stream";

            byte[] bytes = Files.readAllBytes(requestedFile);
            exchange.getResponseHeaders().set("Content-Type", mime);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (IOException e) {
            sendSafely(exchange, 500, "500 Internal Server Error");
        }

        return "";
    }, new String[] { "GET" });
}

public void serveStaticContext(String folderPath){
    serveStaticContext(folderPath, "/");
}

public void serveStaticContext(String folderPath, String routePrefix) {
    Path basePath = Paths.get(folderPath).toAbsolutePath().normalize();
    String cleanPrefix = routePrefix.endsWith("/") ? routePrefix : routePrefix + "/";

    this.add(cleanPrefix + ".*", (exchange, params) -> {
        String rawPath = exchange.getRequestURI().getPath();          // full URL path
        String contextPath = exchange.getHttpContext().getPath();     // context, like "/api"
        String relativePath = rawPath.substring(contextPath.length()); // trim context → "/cat.jpg" or "/assets/img.png"

        if (!cleanPrefix.equals("/")) {
            if (!relativePath.startsWith(cleanPrefix)) return null;
            relativePath = relativePath.substring(cleanPrefix.length()); // chop "/assets/"
        } else {
            if (relativePath.startsWith("/")) relativePath = relativePath.substring(1); // chop "/"
        }

        Path fileToServe = basePath.resolve(relativePath).normalize();

        // Security: block attempts to escape base folder
        if (!fileToServe.startsWith(basePath)) {
            sendSafely(exchange, 403, "403 Forbidden");
            return "";
        }

        if (!Files.exists(fileToServe) || Files.isDirectory(fileToServe)) {
            return null;
        }

        try {
            String mime = Files.probeContentType(fileToServe);
            if (mime == null) mime = "application/octet-stream";

            byte[] bytes = Files.readAllBytes(fileToServe);
            exchange.getResponseHeaders().set("Content-Type", mime);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (IOException e) {
            sendSafely(exchange, 500, "500 Internal Server Error");
        }

        return "";
    }, new String[]{"GET"});
}

// Helper to handle errors silently
    private void sendSafely(HttpExchange exchange, int status, String message) {
        try {
            send(exchange, status, message);
        } catch (IOException ignored) {}
    }

    public void listen(String host, int port) {
        listen("/", host, port, false);
    }

    public void listen(String host, int port, boolean output) {
        listen("/", host, port, output);
    }
    // 🔧 Internal method to reduce repetition
    private void listen(String context, String host, int port, boolean output) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
            server.createContext(context, this::handle);
            server.setExecutor(null); // default executor
            server.start();
            if (output) {
                System.out.println("Server running at http://" + host + ":" + port + context);
            }
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }

    public void listenContext(String context, String host, int port) {
        listenOut(context, host, port, false);
    }

    public void listenContext(String context, String host, int port, boolean output) {
        listenOut(context, host, port, output);
    }

    private void listenOut(String context, String host, int port, boolean output) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
            server.createContext(context, this::handleContext);
            server.setExecutor(null); // default executor
            server.start();
            if (output) {
                System.out.println("Server running at http://" + host + ":" + port + context);
            }
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }


}
