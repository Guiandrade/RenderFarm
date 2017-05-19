package com.ist.cnv.decider;

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

import java.util.concurrent.ConcurrentHashMap;

import com.ist.cnv.matrixes.*;


public class Decider {
  static AWSCredentials credentials = new ProfileCredentialsProvider().getCredentials();
  static int TIMEOUT = 20;
  static int TIMEWINDOW= 15000;
  static ConcurrentHashMap<String,List<Map<String,AttributeValue>>> listParams = new ConcurrentHashMap<String,List<Map<String,AttributeValue>>>();
  static HashSet<String> ips = new HashSet<String>();
  static private ConcurrentHashMap<String,Long> paramsMap = new ConcurrentHashMap<String,Long>();
  static private ConcurrentHashMap<String,ConcurrentHashMap<Long,Long>> ipsTimes = new ConcurrentHashMap<String,ConcurrentHashMap<Long,Long>>();
  static private long ID = 0L;
  static Decider decider = null;

  public Decider(){
  	new Thread(new HealthCheckRunnable()).start();
	new Thread(new InspectDBRunnable()).start();
  }

  public static Decider getInstance(){
  	if(decider == null){
  		decider = new Decider();
  	}
  	return decider;
  }

  public static class HealthCheckRunnable implements Runnable {

    public void run() {

      while(true){
        try{
          healthCheck();
          for(String str : ips){
            System.out.println("IP: " + str);
          }
          System.out.println();
          Thread.sleep(TIMEWINDOW);
        }catch (Exception e){
          e.printStackTrace();
        }
      }
    }
  }

  public static class InspectDBRunnable implements Runnable {

    public void run() {
      while(true){
        try{
          int numFiles=5;
          for (int i=0;i<numFiles;i++){
            listParams.put(String.valueOf(i+1),com.ist.cnv.dynamoDB.DynamoDB.getInstance().scan("params",String.valueOf(i+1)).getItems());
          }
          listParams.put("times",com.ist.cnv.dynamoDB.DynamoDB.getInstance().scan("times",null).getItems());
          //System.out.println("DATABASE DATA  UPDATED!");
          Thread.sleep(TIMEWINDOW);
        }catch (Exception e){
          e.printStackTrace();
        }
      }
    }
  }


  public static void healthCheck() throws Exception{
    HashSet<String> ipsRetrieved = getInstancesIps();
    HashSet<String> runningIps = new HashSet<String>();
    for(String ip : ipsRetrieved){
      String response = executePost("http://"+ip+":8000/test");
      if((response.split("\n")[0]).equals("Command OK: Load Balancer Health Check / Keep Alive ")){
        runningIps.add(ip);
      }
    }
    ips = runningIps;
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

  public static HashSet<String> getInstancesIps(){
    HashSet<String> ips = new HashSet<String>();
    try{
      AmazonEC2 ec2;
      ec2 = AmazonEC2ClientBuilder.standard().withRegion("eu-west-1").withCredentials(new AWSStaticCredentialsProvider(credentials)).build();

      DescribeInstancesResult describeInstancesRequest = ec2.describeInstances();
      List<Reservation> reservations = describeInstancesRequest.getReservations();
      Set<Instance> instances = new HashSet<Instance>();

      for (Reservation reservation : reservations) {
        instances.addAll(reservation.getInstances());
      }

      for(Instance instance : instances){
        if(instance.getState().getName().equals("running")){
          if(instance.getSecurityGroups().get(0).getGroupName().equals("CNV-ssh+http")){
            ips.add(instance.getPublicIpAddress());
          }
        }
      }
    }catch (AmazonServiceException ase){
      com.ist.cnv.dynamoDB.DynamoDB.getInstance().printASE(ase);
    }catch (Exception e){
      e.printStackTrace();
    }

    return ips;
  }

  public static long getNumEstimatedInstructions() throws NoSquareException{
    int numFiles=5;
    String filePos = String.valueOf(paramsMap.get("file").intValue());
    List<Map<String,AttributeValue>> listParamsFile;
    if(listParams.containsKey(filePos)){
      listParamsFile = listParams.get(filePos);
    }
    else{return 0L;} // not enough values from given file
    if (listParamsFile.size()<6){return 0L;} // not enough values to make regression
    int numParameters = 3;
    double[][] paramValues = new double [listParamsFile.size()][numParameters];
    double[][] instructionsValues = new double [listParamsFile.size()][1];
    int i=0;

    for(Map<String,AttributeValue> map : listParamsFile){
      for(String str : map.keySet()){
        if(str.equals("id") || str.equals("file")){
          continue;
        }
        else if (str.equals("sc") || str.equals("sr")){
          if(paramValues[i][0] != 0){
            paramValues[i][0]*=Double.valueOf(map.get(str).getN())+getRandomDouble();
          }
          else{
            paramValues[i][0]=Double.valueOf(map.get(str).getN())+getRandomDouble();
          }
        }
        else if(str.equals("wc") || str.equals("wr")){
          if(paramValues[i][1] != 0){
            paramValues[i][1]*=Double.valueOf(map.get(str).getN())+getRandomDouble();
          }
          else{
            paramValues[i][1]=Double.valueOf(map.get(str).getN())+getRandomDouble();
          }
        }
        else if(str.equals("coff") || str.equals("roff")){
          if(paramValues[i][2]!= 0){
            paramValues[i][2]*=Double.valueOf(map.get(str).getN())+getRandomDouble();
          }
          else{
            paramValues[i][2]=Double.valueOf(map.get(str).getN())+getRandomDouble();
          }
        }
        else{
          // add number of instructions
          instructionsValues[i][0]=Long.parseLong(map.get(str).getN())+getRandomDouble();
        }
      }
      i++;
    }

    Matrix paramMatrix = new Matrix(paramValues);
    Matrix instructionsMatrix = new Matrix(instructionsValues);
    MultiLinear ml = new MultiLinear(paramMatrix, instructionsMatrix);
    Matrix betas = ml.calculate();

    long result = getEstimatedValue(betas,-1);
    //System.out.println("Estimated Instructions = "+result+"\n");
    //System.out.println("Real Instructions = 4411947001\n");

    // will return Estimate Num Instructions
    return result;
  }

  public static long getEstimatedValue(Matrix betas,long estimatedInst){
    double[] betaValues = new double[betas.getNrows()];
    Double result = 0.0;
    for(int k=0;k<betas.getNrows();k++){
      betaValues[k]=betas.getValueAt(k,0);
    }

    if(estimatedInst == -1){
      long sc = paramsMap.get("sc");
      long sr = paramsMap.get("sr");
      long wc = paramsMap.get("wc");
      long wr = paramsMap.get("wr");
      long coff = paramsMap.get("coff");
      long roff = paramsMap.get("roff");

      result = betaValues[0]+betaValues[1]*(sc*sr)+betaValues[2]*(wc*wr)+betaValues[3]*(coff*roff);
    }

    else{
      result = betaValues[0]+betaValues[1]*estimatedInst;
    }

    return result.longValue();
  }

  public static void getParams(String query){
    String[] params = query.split("&");
    int i=0;
    for (String param : params){
      String value = param.split("=")[1];
      if(i == 0){
        String modelNum = value.split("test0")[1];
        paramsMap.put("file",Long.parseLong(modelNum));
      }
      else{
        if(i == 1){
          paramsMap.put("sc",Long.parseLong(value));
        }
        else if(i == 2){
          paramsMap.put("sr",Long.parseLong(value));
        }
        else if(i == 3){
          paramsMap.put("wc",Long.parseLong(value));
        }
        else if(i == 4){
          paramsMap.put("wr",Long.parseLong(value));
        }
        else if(i == 5){
          paramsMap.put("coff",Long.parseLong(value));
        }
        else if(i == 6){
          paramsMap.put("roff",Long.parseLong(value));
        }
      }
      i++;
    }
  }

  public static double getRandomDouble() {
    // to avoid null determinant when making regression
    double leftLimit = 0.1;
    double rightLimit = 0.2;
    double generatedDouble = leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
    return generatedDouble;
  }

  public static long getEstimatedTime(long instructions) throws NoSquareException{
    int numParameters=1;
    List<Map<String,AttributeValue>> listTimes;
    if (!listParams.containsKey("times")){
      System.out.println("Not enough Values to estimate Time!\n");
      return 0L;
    }
    listTimes = listParams.get("times");
    if(listTimes.size() == 0){return 0L;}
    double[][] instructionsValues = new double [listTimes.size()][numParameters];
    double[][] timesValues = new double [listTimes.size()][1];

    int i=0;
    for(Map<String,AttributeValue> map : listTimes){
      for(String str : map.keySet()){
        if (str.equals("instructions")){
          instructionsValues[i][0]=Long.parseLong(map.get(str).getN())+getRandomDouble();
        }
        else if(str.equals("time")){
          timesValues[i][0]=Long.parseLong(map.get(str).getN())+getRandomDouble();
        }
      }
      i++;
    }

    Matrix instructionsMatrix = new Matrix(instructionsValues);
    Matrix paramMatrix = new Matrix(timesValues);
    MultiLinear ml = new MultiLinear(instructionsMatrix, paramMatrix);
    Matrix betas = ml.calculate();

    long result = getEstimatedValue(betas,instructions);
    //System.out.println("Estimated Time = "+result+" ms\n");
    // will return Estimate Time
    return result;
  }

  public static String sendRequest(long estimatedTime){
    String serverIp = "";
    if (ips.size() == ipsTimes.size()){
      serverIp = selectServerIp();
	  Date date = new Date();
      ConcurrentHashMap<Long,Long> ipTimes = ipsTimes.get(serverIp);
      ipTimes.put(getId(),date.getTime() + estimatedTime);
      ipsTimes.put(serverIp,ipTimes);
      // Send to this ip

      // same number of servers
      //serverIp = selectServerIp();
      // Send to this ip
    }
    else if( ips.size() > ipsTimes.size()){
      // new server
      for(String ip : ips){
        if (!ipsTimes.containsKey(ip)){
          Date date = new Date();
          ConcurrentHashMap<Long,Long> ipTimes = new ConcurrentHashMap<Long,Long>();
          ipTimes.put(getId(),date.getTime() + estimatedTime);
          ipsTimes.put(ip,ipTimes);
          serverIp = ip;
          // Send to this ip
        }
      }

    }
    else{
      // server(s) disconnected
      for(String ip : ipsTimes.keySet()){
        if(!ips.contains(ip)){
          ipsTimes.remove(ip);
          break;
        }
        serverIp = selectServerIp();
        // Send to this ip
      }
    }
    return serverIp;
  }

  public static String selectServerIp(){
  	ConcurrentHashMap<String,Long> timeProcesses = new ConcurrentHashMap<String,Long>();
    for(String ip : ipsTimes.keySet()){
      timeProcesses.put(ip,0L);
      for(Long id : ipsTimes.get(ip).keySet()){
      	System.out.println("tempos " + ipsTimes.get(ip).get(id));

        Date date = new Date();
        System.out.println("data de agora " + date.getTime());
        if (ipsTimes.get(ip).get(id) <= date.getTime()){
          ipsTimes.get(ip).remove(id);
        }
        else{
          long old = timeProcesses.get(ip);
          timeProcesses.put(ip,old+ipsTimes.get(ip).get(id));
        }
      }
    }
    long shortestTime = -1L;
    String serverIp="";
    for (String ip : timeProcesses.keySet()){
      if (shortestTime == -1L){
        shortestTime = timeProcesses.get(ip);
        serverIp=ip;
      }
      else if (shortestTime > timeProcesses.get(ip)){
        shortestTime=timeProcesses.get(ip);
        serverIp=ip;
      }
    }
    for (Map.Entry<String, Long> entry : timeProcesses.entrySet()) {
    	String key = entry.getKey();
    	Object value = entry.getValue();
    	System.out.println("IP : " + key + " || Time: " + value);
	}
    return serverIp;
  }

  public static synchronized long getId() {
    ID++;
    return ID;
  }
}