import java.io.*;
import java.lang.Runtime;
import java.net.InetSocketAddress;


import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class WebServer {


    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/test", new MyHandler());
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool()); //server will run in parallel, non-limited Executor.
        server.start();
        System.out.println("Server is ready! \n");
    }

    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = t.getRequestURI().getQuery();

            // nao bomba este comando que devia : $java -Djava.awt.headless=true -cp src raytracer.Main test05.txt test05.bmp 1000 500 1000 500 50 50
	        System.out.println(execCmd("ls"));

            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    public static String getParams(String query){
        String[] params = query.split("&");
        String response = "java -Djava.awt.headless=true -cp src raytracer.Main test05.txt test05.bmp ";  
        for (String param : params)  
        {  
            String value = param.split("=")[1];  
            response  = response + value + " "; 
        }  
        return response; 

    }
   
    public static String execCmd(String cmd) throws java.io.IOException {
        java.util.Scanner s = new java.util.Scanner(Runtime.getRuntime().exec(cmd).getInputStream()).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
