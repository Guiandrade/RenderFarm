package com.ist.cnv;

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

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import com.ist.cnv.handlers.*;
import com.ist.cnv.matrixes.*;

public class LoadBalancer {

	static AWSCredentials credentials = new ProfileCredentialsProvider().getCredentials();
	static int TIMEOUT = 20;
	static int TIMEWINDOW= 1000;
	static ConcurrentHashMap<String,List<Map<String,AttributeValue>>> listParams = new ConcurrentHashMap<String,List<Map<String,AttributeValue>>>();
	static HashSet<String> ips = new HashSet<String>();
	static private ConcurrentHashMap<String,Long> paramsMap = new ConcurrentHashMap<String,Long>();
	static private ConcurrentHashMap<String,ConcurrentHashMap<Long,String>> ipsTimes = new ConcurrentHashMap<String,ConcurrentHashMap<Long,String>>();
	static private long ID = 0L;


	public static void main(String[] args) throws Exception,NoSquareException {
		HttpServer server = HttpServer.create(new InetSocketAddress(7556), 0);
		server.createContext("/r.html", new RequestHandler());
		server.createContext("/image", new RetrieveImageHandler());
		server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool()); //server will run in parallel, non-limited Executor.
		server.start();
		new Thread(new HealthCheckRunnable()).start();
		new Thread(new InspectDBRunnable()).start();
		System.out.println("Server is ready! \n");
		String query = "http://cnv-lab-aws-lb-1328451237.eu-west-1.elb.amazonaws.com/r.html?f=test05&sc=1000&sr=500&wc=1000&wr=500&coff=40&roff=40";
		getParams(query);
		long time = 0L;
		while (true){
			long estimatedInstructions = getNumEstimatedInstructions();
			if (estimatedInstructions > 0){
					time = getEstimatedTime(estimatedInstructions);
			}
		}
		// Check server with less time with processes and forward request


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
							System.out.println("DATABASE DATA  UPDATED!");
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
		System.out.println("Estimated Instructions = "+result+"\n");
		System.out.println("Real Instructions = 4411947001\n");

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
		System.out.println("Estimated Time = "+result+" ms\n");
		// will return Estimate Time
		return result;
	}

	public void sendRequest(long estimatedTime){
		if (ips.size() == ipsTimes.size()){

		}
		else if( ips.size() > ipsTimes.size()){
			// new server

		}
		else{
			// server(s) disconnected
		}
	}

	public synchronized long getId() {
		ID++;
		return ID;
	}
}
