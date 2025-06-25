import Modulo.*;

public class Main {
    public static void main(String[] args) throws Exception {
        Router router = new Router();

        router.add("/", (req, q)->{
            return Renderer.render("index");
        }, new String[]{"GET"});

        router.serveStatic("public");
        router.listen("127.0.0.1", 8080, true);
    }
}
