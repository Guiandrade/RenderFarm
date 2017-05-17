package com.ist.cnv.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.File;
import java.nio.file.Files;

import java.awt.image.BufferedImage;

public class RetrieveImageHandler implements HttpHandler {
  static int TIMEOUT = 20;

        @Override
        public void handle(HttpExchange t) throws IOException {

        	String query = t.getRequestURI().getQuery();
        	String outputPath ="/home/ec2-user/raytracer-master/outputs/";
        	String modelName = getModelName(query) + ".bmp";
        	String path = outputPath + modelName;
		File file = new File(path);
		t.getResponseHeaders().set("Content-Type","image/bmp");
		t.sendResponseHeaders(200, file.length());

		OutputStream outputStream=t.getResponseBody();
		Files.copy(file.toPath(), outputStream);
		outputStream.close();
        }

        public static String getModelName(String query){
        	// Retrieves only parameter sent on URL

        	String[] params = query.split("=");
        	String value = params[1];
        	return value;
        }
}
