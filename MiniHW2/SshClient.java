import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;


public class SshClient {

	public static void main(String[] args) throws InterruptedException {
		// TODO Auto-generated method stub
		String result = "";
		JSch jsch = new JSch();
		Session session = null;
		Channel openChannel = null;
		String keyDir = "/Users/Modoka/Desktop/JavaEC2Key4.pem";
		String user = "ec2-user";
		String host = "ec2-54-165-58-50.compute-1.amazonaws.com";
		String command = "/sbin/ifconfig;ls -a;";
		try {
			jsch.addIdentity(keyDir);
			}
		catch (JSchException e){
			result+=e.getMessage();
			}
		try {
				session = jsch.getSession(user, host, 22);
				Properties config = new Properties();
				config.put("StrictHostKeyChecking", "no");
				session.setConfig(config);
				session.connect();
				
				System.out.println("SSH Connection Established.");
				
				openChannel= session.openChannel("exec");
				
				((ChannelExec)openChannel).setCommand(command);
				openChannel.setInputStream(null);
				((ChannelExec)openChannel).setErrStream(System.err);
				
				int exitStatus = openChannel.getExitStatus();
				System.out.println("exit-status: "+exitStatus);
				
			
				InputStream in = openChannel.getInputStream();
//				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				
				openChannel.connect();
	
//				String buf = null;
//				while ((buf=reader.readLine()) != null){
//					System.out.println(buf);
//				}
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
			result+= e.getMessage();
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
		
		
		System.out.println(result);
	}

}
