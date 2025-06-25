package Modulo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.*;

public class Renderer {

    public static String render(String fileName) {
        return render(fileName, new String[0], new String[0]);
    }

    public static String render(String fileName, String[] keys, String[] values) {
        try {
            String content = new String(Files.readAllBytes(Paths.get("templates", fileName + ".html")));
            for (int i = 0; i < keys.length; i++) {
                content = content.replace("{{" + keys[i] + "}}", values[i]);
            }
            return content;
        } catch (IOException e) {
            return "<h1>Template Error:</h1><pre>" + e.getMessage() + "</pre>";
        }
    }

    public static void redirect(String path){
        redirect(path, null, null);
    }

  public static void redirect(String pathTemplate, String[] keys, String[] values) {
        Matcher matcher = Pattern.compile(":([a-zA-Z][a-zA-Z0-9_]*)").matcher(pathTemplate);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = "";

            // Search in keys array
            for (int i = 0; i < keys.length; i++) {
                if (keys[i].equals(key)) {
                    replacement = values[i];
                    break;
                }
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        Context.redirect(result.toString());
    }

}
