package com.zireck.remotecraft;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.google.common.net.InetAddresses;

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
		
		System.out.println("k9d3 Estoy dentro del runnable! run() antes del try {");
		
		try {
			socket = new DatagramSocket(PORT, InetAddress.getByName("0.0.0.0"));
			socket.setBroadcast(true);
			
			// better like this
			/*socket = new DatagramSocket(null);
			socket.setBroadcast(true);
			socket.setReuseAddress(true);
			InetSocketAddress ia = new InetSocketAddress(InetAddress.getByName("0.0.0.0"), PORT);
			socket.bind(ia);*/
			
			System.out.println("k9d3 DatagramSocket hecho y preparado!");
			
			int lol = 0;
			
			while (!Thread.currentThread().isInterrupted() && socket.isBound()) {
				// Keep an UDP Socket open
				
				System.out.println("k9d3 iteracion: "+lol);
				lol++;
				
				// Ready to receive sockets
				
				// Receive a packet
				byte[] rcvBuff = new byte[15000];
				DatagramPacket packet = new DatagramPacket(rcvBuff, rcvBuff.length);
				System.out.println("k9d3 Esperando peticion");
				socket.receive(packet);
				System.out.println("k9d3 Peticion recibida!");
				
				// Packet received
				String msg = new String(packet.getData()).trim();
				if (msg.equals("REMOTECRAFT_DISCOVERY_REQUEST")) {
					// Send world name
					String msgResponse = "REMOTECRAFT_DISCOVERY_RESPONSE:"+this.worldName;
					byte[] sendData = msgResponse.getBytes();
					
					// send response
					System.out.println("k9d3 Preparando respuesta");
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
					System.out.println("k9d3 Enviando paquete");
					if (socket.isBound()) {
						try {
							socket.send(sendPacket);
							System.out.println("k9d3 Paquete enviado segurisimo! :)");
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						System.out.println("k9d3 Paquete NO enviado segurisimo! :(");
						continue;
					}
					System.out.println("k9d3 Paquete enviado!");
				}
			}
			
			if (!socket.isBound()) {
				System.out.println("k9d3 Se sale del while porque socket is not bound");
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			System.out.println("k9d3 Desconectado socket UDP!");
			socket.disconnect();
			socket.close();
		}
		
	}
	
}
