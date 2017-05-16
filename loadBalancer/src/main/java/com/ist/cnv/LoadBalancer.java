package com.ist.cnv;

import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

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

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.io.*;
import java.net.*;


public class LoadBalancer {

	static AWSCredentials credentials = new ProfileCredentialsProvider().getCredentials();
	static HashSet<String> ips = new HashSet<String>();
	static int TIMEOUT = 20;

	public static void main(String[] args) throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(7556), 0);
		server.createContext("/r.html", new RequestHandler());
		server.createContext("/image", new com.ist.cnv.handlers.RetrieveImageHandler());
		server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool()); //server will run in parallel, non-limited Executor.
		server.start();
		System.out.println("Server is ready! \n");
		healthCheck();
		com.ist.cnv.dynamoDB.DynamoDB.getInstance().scan("params");
	}

	static class RequestHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {
			String query = t.getRequestURI().getQuery();
			System.out.println("RequestHandler");
			String response = "";
			try{
				for(String ip : ips){
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
			printASE(ase);
		}catch (Exception e){
			e.printStackTrace();
		}

		return ips;
	}

	private static void printASE(AmazonServiceException ase){
		ase.printStackTrace();
		System.out.println("Caught an AmazonServiceException, which means your request made it "
		+ "to AWS, but was rejected with an error response for some reason.");
		System.out.println("Error Message:    " + ase.getMessage());
		System.out.println("HTTP Status Code: " + ase.getStatusCode());
		System.out.println("AWS Error Code:   " + ase.getErrorCode());
		System.out.println("Error Type:       " + ase.getErrorType());
		System.out.println("Request ID:       " + ase.getRequestId());
	}

	private static void printACE(AmazonClientException ace){
		ace.printStackTrace();
		System.out.println("Caught an AmazonClientException, which means the client encountered "
		+ "a serious internal problem while trying to communicate with AWS, "
		+ "such as not being able to access the network.");
		System.out.println("Error Message: " + ace.getMessage());
	}
}
