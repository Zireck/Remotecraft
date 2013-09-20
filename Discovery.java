package com.zireck.remotecraft;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Discovery implements Runnable {
	
	DatagramSocket socket;
	boolean keepRunning = true;
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		try {
			// Keep an UDP Socket open
			socket = new DatagramSocket(8888, InetAddress.getByName("0.0.0.0"));
			socket.setBroadcast(true);
			
			while (keepRunning) {
				// Ready to receive sockets
				
				// Receive a packet
				byte[] rcvBuff = new byte[15000];
				DatagramPacket packet = new DatagramPacket(rcvBuff, rcvBuff.length);
				socket.receive(packet);
				
				// Packet received
				String msg = new String(packet.getData()).trim();
				if (msg.equals("REMOTECRAFT_DISCOVERY_REQUEST")) {
					byte[] sendData = "REMOTECRAFT_DISCOVERY_RESPONSE".getBytes();
					
					// send response
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
					socket.send(sendPacket);
				}
			} // while
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void terminate() {
		keepRunning = false;
	}
	
	// Singleton
	public static Discovery getInstance() {
		return DiscoveryHolder.INSTANCE;
	}
	
	private static class DiscoveryHolder {
		private static final Discovery INSTANCE = new Discovery();
	}

}
