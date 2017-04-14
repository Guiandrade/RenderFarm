#!/bin/sh

# Download and compiling the raytracer folder and creation of output folder for rendered images
cd
wget "http://groups.ist.utl.pt/meic-cnv/project/raytracer-master.tgz"
tar -zxvf raytracer-master.tgz
rm raytracer-master.tgz
cd raytracer-master
mkdir outputs
make -C /home/ec2-user/raytracer-master clean
make -C /home/ec2-user/raytracer-master

# Download and compiling of the BIT folder + adding and compiling our Instrument function
cd
wget "http://grupos.tecnico.ulisboa.pt/~meic-cnv.daemon/labs/labs-bit/BIT.zip"
unzip BIT.zip
rm BIT.zip
cp Instrument.java /home/ec2-user/BIT/samples
cd
rm Instrument.java
source /home/ec2-user/BIT/java-config.sh
javac /home/ec2-user/BIT/samples/*.java

# Compile the WebServer class and handlers
cd
javac /home/ec2-user/server/Webserver.java
javac /home/ec2-user/server/handlers/*.java

# Use our Istrument function to instrument raytracer
cd /home/ec2-user/raytracer-master/src/raytracer
java Instrument .
cd /home/ec2-user/raytracer-master/src/raytracer/pigments
java Instrument .
cd /home/ec2-user/raytracer-master/src/raytracer/shapes
java Instrument .
cd
