package com.ist.cnv.handlers;

import com.ist.cnv.matrixes.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.*;
import java.util.*;
import java.net.InetSocketAddress;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import java.io.File;
import java.nio.file.Files;
import java.awt.image.BufferedImage;

import java.util.concurrent.ConcurrentHashMap;

import com.ist.cnv.matrixes.*;
import com.ist.cnv.decider.*;


public class RequestHandler implements HttpHandler {
  static AWSCredentials credentials = new ProfileCredentialsProvider().getCredentials();
  static int TIMEOUT = 20;
  static int TIMEWINDOW= 15000;
  static ConcurrentHashMap<String,List<Map<String,AttributeValue>>> listParams = new ConcurrentHashMap<String,List<Map<String,AttributeValue>>>();
  static HashSet<String> ips = new HashSet<String>();
  static private ConcurrentHashMap<String,Long> paramsMap = new ConcurrentHashMap<String,Long>();
  static private ConcurrentHashMap<String,ConcurrentHashMap<Long,Long>> ipsTimes = new ConcurrentHashMap<String,ConcurrentHashMap<Long,Long>>();
  static private long ID = 0L;

  @Override
  public void handle(HttpExchange t) throws IOException {
    String query = t.getRequestURI().getQuery();
    System.out.println("RequestHandler");
    //getParams(query);
    String response = "";
    try{
      Decider.getInstance().getParams(query);
      long time = 0L;
      long estimatedInstructions = Decider.getInstance().getNumEstimatedInstructions();
      if (estimatedInstructions > 0){
        time = Decider.getInstance().getEstimatedTime(estimatedInstructions);
      }
      String ip = Decider.getInstance().sendRequest(time);
      System.out.println("DECIDED IP: " + ip);
      response = executePost("http://"+ip+":8000/r.html?"+query);
      java.nio.file.Path path = java.nio.file.Paths.get(response);
      t.getResponseHeaders().set("Content-Type","image/bmp");
      t.sendResponseHeaders(200, response.length());
      OutputStream os = t.getResponseBody();
      Files.copy(path, os);
      os.close();
    }catch (Exception e){
      //e.printStackTrace();
    }
  }

  public static String executePost(String url) throws Exception {
    URL conn = new URL(url);
    URLConnection yc = conn.openConnection();
    try{
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