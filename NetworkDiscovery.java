package com.zireck.remotecraft;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class NetworkDiscovery implements Runnable {
	
	private final int PORT = 9998;
	
	DatagramSocket socket;
	
	boolean keepRunning = true;
	
	String worldName = "";
	
	// Private constructor for singleton
	private NetworkDiscovery() {
		
	}
	
	// Singleton holder
	private static class DiscoveryHolder {
		private static final NetworkDiscovery INSTANCE = new NetworkDiscovery();
	}

	public static NetworkDiscovery getInstance() {
		return DiscoveryHolder.INSTANCE;
	}
	
	// Load the world name
	public void init(String worldName) {
		this.worldName = worldName;
	}

	// Discovery thread
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		try {
			// Keep an UDP Socket open
			socket = new DatagramSocket(PORT, InetAddress.getByName("0.0.0.0"));
			socket.setBroadcast(true);
			
			while (!Thread.currentThread().isInterrupted()) {
				// Ready to receive sockets
				
				// Receive a packet
				byte[] rcvBuff = new byte[15000];
				DatagramPacket packet = new DatagramPacket(rcvBuff, rcvBuff.length);
				//System.out.println("k9d3 waiting to receive packet");
				socket.receive(packet);
				//System.out.println("k9d3 packet received!");
				
				// Packet received
				String msg = new String(packet.getData()).trim();
				if (msg.equals("REMOTECRAFT_DISCOVERY_REQUEST")) {
					// Send world name
					String msgResponse = "REMOTECRAFT_DISCOVERY_RESPONSE:"+this.worldName;
					byte[] sendData = msgResponse.getBytes();
					
					// send response
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
					socket.send(sendPacket);
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
