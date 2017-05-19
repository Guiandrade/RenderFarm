#!/bin/sh

# Update the machine
sudo yum update

# Generate the Web Server jar
cd /home/ec2-user/loadBalancer
mvn clean compile assembly:single

# Put the following command in /etc/rc.local to start the server on machine startup
echo "java -cp /home/ec2-user/loadBalancer/target/renderFarm-1.0-SNAPSHOT-jar-with-dependencies.jar com.ist.cnv.LoadBalancer" | sudo tee -a /etc/rc.local
