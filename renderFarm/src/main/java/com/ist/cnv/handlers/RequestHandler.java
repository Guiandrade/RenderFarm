package com.ist.cnv.handlers;

import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import java.io.*;
import java.lang.Runtime;
import java.util.Date;


public class RequestHandler implements HttpHandler {
    
    private static String urlImage ="cnv-lab-aws-lb-1328451237.eu-west-1.elb.amazonaws.com/image?f=";
    private static int id = 0;
    private static String finished = "Error";
    private static String modelName = "";
    private static DynamoDB db = null;
        
    @Override
    public void handle(HttpExchange t) throws IOException {
        Date date1 = new Date();
        String response = "null";
        String query = t.getRequestURI().getQuery();
            
        if (query != null){
            String command = getParams(query);
            String cmdResponse = "";
            try{
                cmdResponse = execCmd(command.split("split")[0],command.split("split")[1],date1);
            }catch(Exception e){
                e.printStackTrace();
            }
            response = finished + "\n" + cmdResponse;
            finished = "Error"; 
        }

        t.sendResponseHeaders(200, response.length());
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    public static String execCmd(String cmd, String inputParams, Date date1) throws java.io.IOException, Exception {
    	// Executes raytracer with the parameters obtained on the GET request 
    	// and retrieves a link to the created image 

       	java.util.Scanner s = new java.util.Scanner(Runtime.getRuntime().exec(cmd).getInputStream()).useDelimiter("\\A");
        String urlCommand = "curl checkip.amazonaws.com";
        java.util.Scanner urlScanner = new java.util.Scanner(Runtime.getRuntime().exec(urlCommand).getInputStream()).useDelimiter("\\A");
        String next = "";
        String url = "";
        if(s.hasNext()){
            next = s.next();
            url  = urlImage + modelName;
            String dynamicInfoSplitStr = "########## DYNAMIC INFORMATION #########";
            String finishedIn = next.split(dynamicInfoSplitStr)[0];
            createLog(dynamicInfoSplitStr + "\n" + inputParams + next.split(dynamicInfoSplitStr)[1], inputParams, date1);
            finished = "Finished (Id: " + id + ") in: " + finishedIn.split(" ")[finishedIn.split(" ").length - 1];
            id++;
        	return "OK : Here is the resulting image link = "+url;
        }
        else{
            return "Error";
        }
    }

    public static String getParams(String query){
    	// Get parameters from the URL and retrieves the raytracer command that will be executed @ execCms(String cmd)

        String envVariables = ":/home/ec2-user/BIT/samples:java -XX:-UseSplitVerifier";
        String[] params = query.split("&");
        String response = "java -Djava.awt.headless=true -cp /home/ec2-user/raytracer-master/src" + envVariables + " raytracer.Main ";  
        int i=0;
        String inputParams = "";

        for (String param : params)  
        {  
            String value = param.split("=")[1];  
            if(i == 0){
                modelName = value;
                response = response + "/home/ec2-user/raytracer-master/"+modelName+".txt /home/ec2-user/raytracer-master/outputs/"+modelName+".bmp ";    
            }
            else{
                response  = response + value + " ";
                if(i == 1){
                    inputParams = inputParams + "sc   = " + value + "\n";
                }
                else if(i == 2){
                    inputParams = inputParams + "sr   = " + value + "\n";    
                }
                else if(i == 3){
                    inputParams = inputParams + "wc   = " + value + "\n";
                }
                else if(i == 4){
                    inputParams = inputParams + "wr   = " + value + "\n";
                }
                else if(i == 5){
                    inputParams = inputParams + "coff = " + value + "\n";
                }
                else if(i == 6){
                    inputParams = inputParams + "roff = " + value + "\n";
                }
            }    
           	i++;
        }
        inputParams = inputParams + modelName;
        String resp = response + "split" + inputParams;
        return resp; 
    }

    public static synchronized void createLog(String response, String inputParams, Date date1) throws SecurityException,IOException,Exception {

        String filename = "Metrics.txt";
        id++;
        BufferedWriter bw = null;
        FileWriter fw = null;
        Date date = new Date();
        String urlCommand = "curl checkip.amazonaws.com";
        java.util.Scanner urlScanner = new java.util.Scanner(Runtime.getRuntime().exec(urlCommand).getInputStream()).useDelimiter("\\A");
        String machine = "";
        if (urlScanner.hasNext()){
            machine = urlScanner.next();
        }

        try {
            System.out.println(response);

            Map<String,String> pairs = db.getInstance().parserParams(response,inputParams,true,true,true,false,false,false,false,false,false,false,false);
            Map<String, AttributeValue> item = db.getInstance().newItemParams(
                id + " " + machine.split("\n")[0] + " " + date.toString(),
                pairs.get("file"),
                pairs.get("sc"),
                pairs.get("sr"),
                pairs.get("wc"),
                pairs.get("wr"),
                pairs.get("coff"),
                pairs.get("roff"),
                pairs.get("instructions"));
            db.getInstance().addItem(item,"params");

            Date date2 = new Date();
            long seconds = (date2.getTime()-date1.getTime())/1000;
            item = db.getInstance().newItemTimes(
                id + " " + machine.split("\n")[0] + " " + date.toString(),
                response.split("Instructions:   ")[1].split("\n")[0],
                String.valueOf(seconds));
            db.getInstance().addItem(item,"times");

            String data = "Thread (id: " + id + ") || Machine: " + machine + " " + date.toString() + "\n\n" + response + "\n\n";
            File file = new File(filename);

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            // true = append file
            fw = new FileWriter(file.getAbsoluteFile(), true);
            bw = new BufferedWriter(fw);

            bw.write(data);

        } catch (IOException e) {

            e.printStackTrace();

        } finally {

            try {

                if (bw != null)
                    bw.close();

                if (fw != null)
                    fw.close();

            } catch (IOException ex) {

                ex.printStackTrace();

            }
        }
    }
}
