package com.zireck.remotecraft;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

import com.google.common.net.InetAddresses;

public class NetworkDiscovery implements Runnable {
	
	private final int DISCOVERY_PORT = 9998;
	
	private Thread thread;
	
	private DatagramSocket socket;
	
	private String worldName = "";
	
	// Private constructor for singleton
	private NetworkDiscovery() {
		thread = new Thread(this);
		thread.start();
	}
	
	// Singleton holder
	private static class DiscoveryHolder {
		private static final NetworkDiscovery INSTANCE = new NetworkDiscovery();
	}

	public static NetworkDiscovery getInstance() {
		return DiscoveryHolder.INSTANCE;
	}
	
	// Set the world name
	public void setWorldName(String worldName) {
		this.worldName = worldName;
	}
	
	public String getWorldName() {
		return worldName;
	}
	
	public void shutdown() {
		thread.interrupt();
		
		if (socket != null) {
			socket.close();
			socket = null;
		}
		
		thread = null;
	}

	// Discovery thread
	@Override
	public void run() {
		
		while (!Thread.currentThread().isInterrupted()) {
		
			try {
				socket = new DatagramSocket(DISCOVERY_PORT, InetAddress.getByName("0.0.0.0"));
				socket.setBroadcast(true);
				
				// Keep an UDP Socket open
				while (!Thread.currentThread().isInterrupted() && socket.isBound()) {
					// Ready to receive sockets
					
					// Receive a packet
					byte[] rcvBuff = new byte[15000];
					DatagramPacket packet = new DatagramPacket(rcvBuff, rcvBuff.length);
					socket.receive(packet);
					
					// Packet received
					String msg = new String(packet.getData()).trim();
					if (msg.equals("REMOTECRAFT_DISCOVERY_REQUEST")) {
						// Attach world name
						String msgResponse = "REMOTECRAFT_DISCOVERY_RESPONSE:"+this.worldName;
						byte[] sendData = msgResponse.getBytes();
						
						// Send response
						DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
						if (socket.isBound()) {
							try {
								socket.send(sendPacket);
							} catch (Exception e) {
								e.printStackTrace();
							}
						} else {
							continue;
						}
					}
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					socket.disconnect();
					socket.close();
				} catch (NullPointerException e) {
					e.printStackTrace();
				}
			}
		
		} // while !threadIsInterrupted
		
	}
	
}
