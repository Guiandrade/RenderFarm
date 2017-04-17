# RenderFarm
A web  server  elastic  cluster  that  renders  3D  images  on-demand by executing a ray-tracing algorithm (serving as demonstrator of CPU-intensive processing). The system will receive a stream of web requests.  Each request is for the rendering of a rectangle (of specific variable size) that is part of a full, much larger scene.

Here's how to run the project:

1. Create a Load Balancer in Amazon AWS - EC2 Management Console;
2. Create a new linux instance in Amazon AWS - EC2 Management Console;
3. SSH into the created instance and do the following 3 steps in it:
    1. Copy and unzip the "checkpoint_project.zip" file to the instance;
    2. Run the "scriptSetup.sh" placed in /home/ec2-user/server;
    3. Restart the machine;

4. Create an AMI image of that instance in Amazon AWS - EC2 Management Console;
5. Create an Auto Scaler in Amazon AWS - EC2 Management Console using that AMI image and associated with the Load Balancer created previously;
6. Run locally the "scriptTestServer.sh".
