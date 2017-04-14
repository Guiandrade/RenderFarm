import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class WebServer {

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/r.html", new handlers.RequestHandler());
        server.createContext("/test", new handlers.CheckHandler());
        server.createContext("/image", new handlers.RetrieveImageHandler());
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool()); //server will run in parallel, non-limited Executor.
        server.start();
        System.out.println("Server is ready! \n");
    }
 
}