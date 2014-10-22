/*
 * Copyright 2010-2014 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AttachVolumeRequest;
import com.amazonaws.services.ec2.model.AttachVolumeResult;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Volume;
import com.amazonaws.services.ec2.model.VolumeAttachment;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.DomainMetadataRequest;
import com.amazonaws.services.simpledb.model.DomainMetadataResult;
import com.amazonaws.services.simpledb.model.ListDomainsRequest;
import com.amazonaws.services.simpledb.model.ListDomainsResult;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class EbsAttach {

    static AmazonEC2      ec2;
    static String attachedInsId = "i-1f1f3bf1";
    static final String KEYDIR = "/Users/Modoka/Desktop/MacKeyEC2.pem";
   
	static final String publicDNSName= "ec2-54-172-208-184.compute-1.amazonaws.com";
    private static void init() throws Exception {

        /*
         * The ProfileCredentialsProvider will return your [default]
         * credential profile by reading from the credentials file located at
         * (/Users/Modoka/.aws/credentials).
         */
    	
        AWSCredentials credentials = null;
        try {
        	credentials = new PropertiesCredentials(
          			 EbsAttach.class.getResourceAsStream("AwsCredentials.properties"));
//            credentials = new ProfileCredentialsProvider("default").getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                    "location (/Users/Modoka/.aws/credentials), and is in valid format.",
                    e);
        }
        ec2 = new AmazonEC2Client(credentials);
       
    }


    public static void main(String[] args) throws Exception {

        init();

        try {
            DescribeAvailabilityZonesResult availabilityZonesResult = ec2.describeAvailabilityZones();
            System.out.println("You have access to " + availabilityZonesResult.getAvailabilityZones().size() +
                    " Availability Zones.");

            DescribeInstancesResult describeInstancesRequest = ec2.describeInstances();
            List<Reservation> reservations = describeInstancesRequest.getReservations();
            Set<Instance> instances = new HashSet<Instance>();

            for (Reservation reservation : reservations) {
                instances.addAll(reservation.getInstances());
            }

            System.out.println("You have " + instances.size() + " Amazon EC2 instance(s) running.");
            for (Instance ins : instances){
            	
            	// instance id
            	String instanceId = ins.getInstanceId();
            	
            	// instance state
            	InstanceState is = ins.getState();
            	System.out.println(instanceId+" "+is.getName());
            }
            
            System.out.println("Attached Instance id: "+ attachedInsId);
            /*********************************************
             * 
             *  #1 Create New Volume
             *  
             *********************************************/
            System.out.println("===========================================");
            System.out.println("#1	Volume State before Creating");
            System.out.println("===========================================");
            
            DescribeVolumesResult describeVolumesResult = ec2.describeVolumes();
            List<Volume> volumes = describeVolumesResult.getVolumes();
            for (Volume volume : volumes) {
            	String volumeId = volume.getVolumeId();
            	String volumeAvbZone = volume.getAvailabilityZone();
            	String volumeSize = String.valueOf(volume.getSize());
            	String volumeState = volume.getState();
            	System.out.println(volumeId+"\t"+volumeSize+"\t"+volumeAvbZone+"\t"+volumeState);
            }
            
            CreateVolumeRequest createVolumeRequest = new CreateVolumeRequest().
            														withSize(4).
            														withVolumeType("gp2").
            														withAvailabilityZone("us-east-1a");
            CreateVolumeResult createVolumeResult = ec2.createVolume(createVolumeRequest);
            String createdVolumeId = createVolumeResult.getVolume().getVolumeId();
            System.out.println("# Created a volume #  ID: "+ createdVolumeId);
            /*********************************************
             * 
             *  #2 Attach the created new Volume
             *  
             *********************************************/
            System.out.println("===========================================");
            System.out.println("#2	Volume State after Creating");
            System.out.println("===========================================");
            
            describeVolumesResult = ec2.describeVolumes();
            volumes = describeVolumesResult.getVolumes();
            for (Volume volume : volumes) {
            	String volumeId = volume.getVolumeId();
            	String volumeAvbZone = volume.getAvailabilityZone();
            	String volumeSize = String.valueOf(volume.getSize());
            	String volumeState = volume.getState();
            	System.out.println(volumeId+"\t"+volumeSize+"\t"+volumeAvbZone+"\t"+volumeState);
            }
            //wait new volume is available
            System.out.println("#	Waiting new Volume is available");
            boolean avail = false;
            while (true) {
            	Thread.sleep(1000);
            	describeVolumesResult = ec2.describeVolumes();
                volumes = describeVolumesResult.getVolumes();
                for (Volume volume : volumes) {
                	if (volume.getVolumeId().equals(createdVolumeId) && volume.getState().equals("available"))
                		{
                			avail = true;
                			break;
                		}
                	}
                if (avail)
                	break;
            }
            System.out.println("New Volume is available now.");
            
            AttachVolumeRequest attachVolumeRequest = new AttachVolumeRequest()
            											.withVolumeId(createdVolumeId)
            											.withInstanceId(attachedInsId)
            											.withDevice("/dev/sdn");
            AttachVolumeResult attachVolumeResult = ec2.attachVolume(attachVolumeRequest);
            VolumeAttachment volumeAttachment = attachVolumeResult.getAttachment();
            
            System.out.println(volumeAttachment.getVolumeId() + " is attached on "+ volumeAttachment.getAttachTime());
            
            /*********************************************
             * 
             *  #3 SSh to instance and run command
             *  
             *********************************************/
            System.out.println("===========================================");
            System.out.println("#2	Ssh and mount disk");
            System.out.println("===========================================");
            
    		Session session = null;
    		Channel openChannel = null;
    		String keyDir = KEYDIR;
    		String user = "ec2-user";
    		String host = publicDNSName;
    		String command = "df -h;"
    				+ "sudo mkfs -t ext3 /dev/xvdn;"
    				+ "sudo mkdir /minihw3;"
    				+ "sudo mount /dev/xvdn /minihw3;"
    				+ "df -h";
    		boolean isConnect = false;
    		while (!isConnect) {
    			JSch jsch = new JSch();
    			System.out.println("Waiting 5 seconds for SSH being available.");
    			Thread.sleep(5*1000);
    			try {
	    			jsch.addIdentity(keyDir);
	    			}
	    		catch (JSchException e){
	    			System.out.println(e.getMessage());
	    			}
	    		try {
	    				session = jsch.getSession(user, host, 22);
	    				Properties config = new Properties();
	    				config.put("StrictHostKeyChecking", "no");
	    				session.setConfig(config);
	    				session.connect();
	    				System.out.println("SSH Connection Established.");
	    				isConnect= true;
	    				openChannel= session.openChannel("exec");
	    				
	    				((ChannelExec)openChannel).setPty(true);
	    				
	    				((ChannelExec)openChannel).setCommand(command);
	    				openChannel.setInputStream(null);
	    				((ChannelExec)openChannel).setErrStream(System.err);
	    				
	    				int exitStatus = openChannel.getExitStatus();
	    				System.out.println("exit-status: "+exitStatus);		
	    				InputStream in = openChannel.getInputStream();
	    				System.out.println("Exxcute remote cammand :"+command);
	    				openChannel.connect();
	
	    				byte[] tmp=new byte[1024];
	    				while(true){
	    					while(in.available()>0){
	    						int i=in.read(tmp, 0, 1024);
	    						if(i<0)break;
	    						System.out.print(new String(tmp, 0, i));
	    				}
	    					if(openChannel.isClosed()){
	    						if(in.available()>0) continue;
	    						System.out.println("exit-status: "+openChannel.getExitStatus());
	    						break;
	    					}
	    					try {Thread.sleep(1000);} catch (Exception ee){}	
	    				}
	    			}
	    		catch (JSchException | IOException e){
	    			System.out.println(e.getMessage());
	    		}
	    		finally {
	    			if (openChannel !=null && !openChannel.isClosed()) {
	    				openChannel.disconnect();
	    				System.out.println("Channel Disconnected");
	    			}
	    			if (session != null && session.isConnected())
	    			{
	    				session.disconnect();
	    				System.out.println("Session Disconnected");
	    			}
	    		}
	    		
    		}
        } catch (AmazonServiceException ase) {
                System.out.println("Caught Exception: " + ase.getMessage());
                System.out.println("Reponse Status Code: " + ase.getStatusCode());
                System.out.println("Error Code: " + ase.getErrorCode());
                System.out.println("Request ID: " + ase.getRequestId());
        }

    }
}
