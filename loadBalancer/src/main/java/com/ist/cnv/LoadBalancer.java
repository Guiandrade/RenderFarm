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
import java.util.Random;
import java.io.*;
import java.net.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import com.ist.cnv.handlers.*;
import com.ist.cnv.matrixes.*;

public class LoadBalancer {

	static AWSCredentials credentials = new ProfileCredentialsProvider().getCredentials();
	static int TIMEOUT = 20;
	static HashSet<String> ips = new HashSet<String>();

	public static void main(String[] args) throws Exception,NoSquareException {
		HttpServer server = HttpServer.create(new InetSocketAddress(7556), 0);
		server.createContext("/r.html", new RequestHandler());
		server.createContext("/image", new RetrieveImageHandler());
		server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool()); //server will run in parallel, non-limited Executor.
		server.start();
		System.out.println("Server is ready! \n");
		healthCheck();
		getNumEstimate();
	}

	public static double getNumEstimate() throws NoSquareException{
		List<Map<String,AttributeValue>> list = com.ist.cnv.dynamoDB.DynamoDB.getInstance().scan("params").getItems();
		double[][] paramValues = new double [list.size()][3];
		double[][] instructionsValues = new double [list.size()][1];
		int i=0;
		for(Map<String,AttributeValue> map : list){
			int j=0;
			for(String str : map.keySet()){
				if(str.equals("id") || str.equals("file")){
					continue;
				}
				else if (str.equals("sc") || str.equals("coff")){
					if(paramValues[i][0] != 0){
						paramValues[i][0]*=Double.valueOf(map.get(str).getN())+getRandomDouble();
					}
					else{
						paramValues[i][0]=Double.valueOf(map.get(str).getN())+getRandomDouble();
					}
					System.out.println("Key : "+str+ " Value: "+paramValues[i][j]);
				}
				else if(str.equals("roff") || str.equals("wr")){
					if(paramValues[i][1] != 0){
						paramValues[i][1]*=Double.valueOf(map.get(str).getN())+getRandomDouble();
					}
					else{
						paramValues[i][1]=Double.valueOf(map.get(str).getN())+getRandomDouble();
					}
				}
				else if(str.equals("wc") || str.equals("sr")){
					if(paramValues[i][2]!= 0){
						paramValues[i][2]*=Double.valueOf(map.get(str).getN())+getRandomDouble();
					}
					else{
						paramValues[i][2]=Double.valueOf(map.get(str).getN())+getRandomDouble();
					}
				}

				else{
					// add number of instructions
					instructionsValues[i][0]=Double.valueOf(map.get(str).getN())+getRandomDouble();
					if(j!=0){j--;}
				}
				j++;
			}
			System.out.println("\n End Row \n ");
			i++;
		}

		System.out.println("Values of paramsMatrix : "+paramValues+"\n");
		System.out.println("Values of instructionsMatrix : "+instructionsValues +"\n");


		Matrix paramMatrix = new Matrix(paramValues);

		Matrix instructionsMatrix = new Matrix(instructionsValues);

		System.out.println("Num linhas paramMatrix : "+paramMatrix.getNrows()+ " Num colunas paramMatrix : "+paramMatrix.getNcols()+"\n");
		System.out.println("Num linhas instructionsMatrix : "+instructionsMatrix.getNrows()+ " Num colunas paramMatrix : "+instructionsMatrix.getNcols()+"\n");

		MultiLinear ml = new MultiLinear(paramMatrix, instructionsMatrix);
		Matrix betas = ml.calculate();

		System.out.println("\n--- Values obtained for the betas ---");
		long[] betaValues =new long[betas.getNrows()];
		for(int k=0;k<betas.getNrows();k++){
			Double num = betas.getValueAt(k,0);
			System.out.println("b"+k+" : "+num.longValue());
			betaValues[k]=num.longValue();
		}
		System.out.println("--- End of betas ---\n");

		long result = betaValues[0]+betaValues[1]*(80*30)+betaValues[2]*(30*40)+betaValues[3]*(80*40);
		System.out.println("Result : "+result+"\n");
		System.out.println("Real Result : 19919250");

		// will return Estimate Num Instructions
		return 0;
	}

	public static void healthCheck() throws Exception{
		HashSet<String> ipsRetrieved = getInstancesIps();
		for(String ip : ipsRetrieved){
			String response = executePost("http://"+ip+":8000/test");
			if((response.split("\n")[0]).equals("Command OK: Load Balancer Health Check / Keep Alive ")){
				ips.add(ip);
				System.out.println("Ip: "+ip+ " added!");
			}
			if(response.equals("Time out")){
				System.out.println("Time out!");
			}
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
						System.out.println(instance.getPublicIpAddress());
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

	public static double getRandomDouble() {
		double leftLimit = 0.1;
		double rightLimit = 0.2;
		double generatedDouble = leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
		return generatedDouble;
	}


}
