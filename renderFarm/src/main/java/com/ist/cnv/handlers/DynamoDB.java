package com.ist.cnv.handlers;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import com.amazonaws.auth.AWSStaticCredentialsProvider;


public class DynamoDB {

    public static DynamoDB instance = null;
    static AmazonDynamoDBClient dynamoDB;
    static AWSCredentials credentials = null;
    static String tableName = "metrics";

    protected DynamoDB(){
        try{
            init();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static DynamoDB getInstance() {
      if(instance == null) {
         instance = new DynamoDB();
      }
      return instance;
   }

    public static void init() throws Exception {
        
        try {
            credentials = new ProfileCredentialsProvider().getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                    "location (~/.aws/credentials), and is in valid format.",
                    e);
        }
        dynamoDB = new AmazonDynamoDBClient(credentials);
        Region euWest1 = Region.getRegion(Regions.EU_WEST_1);
        dynamoDB.setRegion(euWest1);
    }

    public static void addItem(Map<String, AttributeValue> item, String tableName) throws Exception{
        try {
            PutItemRequest putItemRequest = new PutItemRequest(tableName, item);
            PutItemResult putItemResult = dynamoDB.putItem(putItemRequest);
        } catch (AmazonServiceException ase) {
            printASE(ase);
        } catch (AmazonClientException ace) {
            printACE(ace);
        }
    }

    public static Map<String, AttributeValue> newItemParams(String id, String file, String sc, String sr, String wc, String wr, String coff, String roff, String instructions) {
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put("id", new AttributeValue(id));
        item.put("file", new AttributeValue().withN(file));
        item.put("sc", new AttributeValue().withN(sc));
        item.put("sr", new AttributeValue().withN(sr));
        item.put("wc", new AttributeValue().withN(wc));
        item.put("wr", new AttributeValue().withN(wr));
        item.put("coff", new AttributeValue().withN(coff));
        item.put("roff", new AttributeValue().withN(roff));
        item.put("instructions", new AttributeValue().withN(instructions));
        return item;
    }

    public static Map<String, AttributeValue> newItemTimes(String id, String date, String instructions, String time) {
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put("id", new AttributeValue(id));
        item.put("date", new AttributeValue(date));
        item.put("instructions", new AttributeValue().withN(instructions));
        item.put("time", new AttributeValue().withN(time));
        return item;
    }

    public static Map<String,String> parserParams(String response, String inputParams, boolean instructions, boolean basicBlocks, boolean methods, boolean fieldLoad, boolean fieldStore,
        boolean regularLoad, boolean regularStore, boolean new_, boolean newarray, boolean anewarray, boolean multianewarray){
        Map<String,String> pairs = new HashMap<String,String>();
        pairs.put("sc",inputParams.split("\n")[0].split("sc   = ")[1]);
        pairs.put("sr",inputParams.split("\n")[1].split("sr   = ")[1]);
        pairs.put("wc",inputParams.split("\n")[2].split("wc   = ")[1]);
        pairs.put("wr",inputParams.split("\n")[3].split("wr   = ")[1]);
        pairs.put("coff",inputParams.split("\n")[4].split("coff = ")[1]);
        pairs.put("roff",inputParams.split("\n")[5].split("roff = ")[1]);
        pairs.put("file",inputParams.split("\n")[6].split("test0")[1]);
        if(instructions==true){
            pairs.put("instructions",response.split("Instructions:   ")[1].split("\n")[0]);
        }
        if(basicBlocks==true){
            //pairs.put("basicBlocks",response.split("Basic blocks:   ")[1].split("\n")[0]);
        }
        if(methods==true){
            //pairs.put("methods",response.split("Methods:        ")[1].split("\n\n")[0]);
        }
        if(fieldLoad==true){
            //to do
        }
        if(fieldStore==true){
            //to do
        }
        if(regularLoad==true){
            //to do
        }
        if (regularStore==true){
            //to do
        }
        if (new_==true){
            //to do
        }
        if (newarray==true){
            //to do
        }
        if(anewarray==true){
            //to do
        }
        if(multianewarray==true){
            //to do
        }
        return pairs;
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
