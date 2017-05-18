package com.ist.cnv.handlers;

import com.ist.cnv.matrixes.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;


public class RequestHandler implements HttpHandler {
  static int TIMEOUT = 20;
  static private ConcurrentHashMap<String,Long> paramsMap = new ConcurrentHashMap<String,Long>();

  @Override
  public void handle(HttpExchange t) throws IOException {
    String query = t.getRequestURI().getQuery();
    System.out.println("RequestHandler");
    //getParams(query);
    String response = "";
    try{
      for(String ip : com.ist.cnv.LoadBalancer.getInstancesIps()){
        response = executePost("http://"+ip+":8000/r.html?"+query);
      }
      t.sendResponseHeaders(200, response.length());
      OutputStream os = t.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }catch (Exception e){
      e.printStackTrace();
    }
  }

  public static String executePost(String url) throws Exception {
    URL conn = new URL(url);
    URLConnection yc = conn.openConnection();
    try{
      yc.setConnectTimeout(TIMEOUT * 1000);
      BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
      String inputLine;
      String response = "";
      while ((inputLine = in.readLine()) != null)
      response += inputLine;
      in.close();
      return response;
    } catch (SocketTimeoutException e){
      return "Time out";
    }
  }

}
