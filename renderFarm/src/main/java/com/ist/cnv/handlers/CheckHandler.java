package com.ist.cnv.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;


public class CheckHandler implements HttpHandler {
        
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "Command OK: Load Balancer Health Check / Keep Alive \n";
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
}
    