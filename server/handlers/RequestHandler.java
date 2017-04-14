package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.lang.Runtime;


public class RequestHandler implements HttpHandler {
    
    private static String modelName = "";
    private static String urlImage ="cnv-lab-aws-lb-1328451237.eu-west-1.elb.amazonaws.com/image?f=";
    private static int id = 0;
        
    @Override
    public void handle(HttpExchange t) throws IOException {
        String response = "null";
        String query = t.getRequestURI().getQuery();
            
        if (query != null){
            String command = getParams(query);
            System.out.println("Command: " + command + "\n");
            response = execCmd(command);
        }

        t.sendResponseHeaders(200, response.length());
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    public static String execCmd(String cmd) throws java.io.IOException {
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
            String finished = "Finished (Id: " + id + ") in: " + next.split(" ")[next.split(" ").length - 1];
           // System.out.println(finished); TIAGO REVE O QUE QUERES FAZER AQUI SFF
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

        for (String param : params)  
        {  
            String value = param.split("=")[1];  
            if(i == 0){
                modelName = value;
                response = response + "/home/ec2-user/raytracer-master/"+modelName+".txt /home/ec2-user/raytracer-master/outputs/"+modelName+".bmp ";    
            }
            else{
                response  = response + value + " "; 
            }    

           	i++;
        }  
        return response; 
    }

}
