package handlers;

import java.util.HashMap;
import java.util.Map;

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
import java.math.BigInteger;


public class DynamoDB {

    static AmazonDynamoDBClient dynamoDB;
    static String resp = "Instructions:   116979705\nBasic blocks:   690087872\nMethods:        191242243\n\nLoad Store Summary:\nField load:     611063346\nField store:    150828563\nRegular load:   1730454681\nRegular store:  206327659\n\nAllocations summary:\nnew:            56262553\nnewarray:       4000000\nanewarray:      1000000\nmultianewarray: 500000";
    static String inputParams = "sc   = 1000\nsr   = 500\nwc   = 1000\nwr   = 500\ncoff = 40\nroff = 40";
    static String tableName = "metrics";

    public static void init() throws Exception {
        
        AWSCredentials credentials = null;
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

    public static void main(String[] args) throws Exception {
        init();
        String tableName = "metrics";
        try{
            if(args[0].equals("create")){
                createTable(tableName);
            }
            if(args[0].equals("delete")){
                deleteTable(tableName);
            }

        }catch (Exception e){
            Map<String, String> pairs = parser(resp,inputParams,true,true,true,false,false,false,false,false,false,false,false);
            Map<String, AttributeValue> item = newItem(
                "1 34.252.211.213 Mon Apr 17 17:07:19 UTC 2017",
                "34.252.211.213",
                pairs.get("sc"),
                pairs.get("sr"),
                pairs.get("wc"),
                pairs.get("wr"),
                pairs.get("coff"),
                pairs.get("roff"),
                pairs.get("instructions"),
                pairs.get("basicBlocks"),
                pairs.get("methods"));
            //addItem(item);
            scan(tableName);
        }
    }

    public static void addItem(Map<String, AttributeValue> item) throws Exception{
        try {
	    init();
            PutItemRequest putItemRequest = new PutItemRequest(tableName, item);
            PutItemResult putItemResult = dynamoDB.putItem(putItemRequest);
        } catch (AmazonServiceException ase) {
            printASE(ase);
        } catch (AmazonClientException ace) {
            printACE(ace);
        }
    }

    private static void createTable(String tableName) throws Exception{
        try {
            // Create a table with a primary hash key named 'id', which holds a string
            CreateTableRequest createTableRequest = new CreateTableRequest().withTableName(tableName)
                .withKeySchema(new KeySchemaElement().withAttributeName("id").withKeyType(KeyType.HASH))
                .withAttributeDefinitions(new AttributeDefinition().withAttributeName("id").withAttributeType(ScalarAttributeType.S))
                .withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L));

            // Create table if it does not exist yet
            TableUtils.createTableIfNotExists(dynamoDB, createTableRequest);
            // wait for the table to move into ACTIVE state
            TableUtils.waitUntilActive(dynamoDB, tableName);

            // Describe our new table
            DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(tableName);
            TableDescription tableDescription = dynamoDB.describeTable(describeTableRequest).getTable();
            System.out.println("Table Description: " + tableDescription);

        } catch (AmazonServiceException ase) {
            printASE(ase);
        } catch (AmazonClientException ace) {
            printACE(ace);
        }
    }

    private static void deleteTable(String tableName) throws Exception{
        try {
            DeleteTableRequest deleteTableRequest = new DeleteTableRequest().withTableName(tableName);
            TableUtils.deleteTableIfExists(dynamoDB, deleteTableRequest);

        } catch (AmazonServiceException ase) {
            printASE(ase);
        } catch (AmazonClientException ace) {
            printACE(ace);
        }
    }

    public static void scan(String tableName){
        try {
            // Scan items for runs with methods greater than 10000
            HashMap<String, Condition> scanFilter = new HashMap<String, Condition>();
            Condition condition = new Condition()
                .withComparisonOperator(ComparisonOperator.GT.toString())
                .withAttributeValueList(new AttributeValue().withN("1"));
            scanFilter.put("methods", condition);
            ScanRequest scanRequest = new ScanRequest(tableName).withScanFilter(scanFilter);
            ScanResult scanResult = dynamoDB.scan(scanRequest);
            System.out.println("Result: " + scanResult);

        } catch (AmazonServiceException ase) {
            printASE(ase);
        } catch (AmazonClientException ace) {
            printACE(ace);
        }
    }

    public static Map<String, AttributeValue> newItem(String id, String machine, String sc, String sr, String wc, String wr, String coff, String roff, String instructions, String basicBlocks, String methods) {
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put("id", new AttributeValue(id));
        item.put("machine", new AttributeValue(machine));
        item.put("sc", new AttributeValue().withN(sc));
        item.put("sr", new AttributeValue().withN(sr));
        item.put("wc", new AttributeValue().withN(wc));
        item.put("wr", new AttributeValue().withN(wr));
        item.put("coff", new AttributeValue().withN(coff));
        item.put("roff", new AttributeValue().withN(roff));
        item.put("instructions", new AttributeValue().withN(instructions));
        item.put("basicBlocks", new AttributeValue().withN(basicBlocks));
        item.put("methods", new AttributeValue().withN(methods));
        return item;
    }

    //To go to instrument
    public static Map<String,String> parser(String response, String inputParams, boolean instructions, boolean basicBlocks, boolean methods, boolean fieldLoad, boolean fieldStore,
        boolean regularLoad, boolean regularStore, boolean new_, boolean newarray, boolean anewarray, boolean multianewarray){
        Map<String,String> pairs = new HashMap<String,String>();
        pairs.put("sc",inputParams.split("\n")[0].split("sc   = ")[1]);
        pairs.put("sr",inputParams.split("\n")[1].split("sr   = ")[1]);
        pairs.put("wc",inputParams.split("\n")[2].split("wc   = ")[1]);
        pairs.put("wr",inputParams.split("\n")[3].split("wr   = ")[1]);
        pairs.put("coff",inputParams.split("\n")[4].split("coff = ")[1]);
        pairs.put("roff",inputParams.split("\n")[5].split("roff = ")[1]);
        if(instructions==true){
            pairs.put("instructions",response.split("Instructions:   ")[1].split("\n")[0]);
        }
        if(basicBlocks==true){
            pairs.put("basicBlocks",response.split("Basic blocks:   ")[1].split("\n")[0]);
        }
        if(methods==true){
            pairs.put("methods",response.split("Methods:        ")[1].split("\n\n")[0]);
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
