#!/bin/sh
source /home/ec2-user/BIT/java-config.sh
javac /home/ec2-user/BIT/samples/*.java
make -C /home/ec2-user/raytracer-master clean
make -C /home/ec2-user/raytracer-master
cd /home/ec2-user/raytracer-master/src/raytracer
java Instrument .
cd /home/ec2-user/raytracer-master/src/raytracer/pigments
java Instrument .
cd /home/ec2-user/raytracer-master/src/raytracer/shapes
java Instrument .
cd
