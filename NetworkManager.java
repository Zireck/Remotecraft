package com.zireck.remotecraft;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class NetworkManager implements Runnable {
	
	private final int PORT = 9999;
	
	// Reference to the main class
	Core core;
	
	// Socket Server
	ServerSocket serverSocket;
	Socket clientSocket = null;
	ObjectOutputStream out;
	ObjectInputStream in;
	String msg;
	
	// Self Thread
	Thread thread;

	// This class needs to stop running
	boolean keepRunning = true;
	
	// Tells you if there's a currently established socket connection
	boolean connectivity = false;
	
	public NetworkManager(Core core) {
		// TODO Auto-generated constructor stub
		this.core = core;

		setKeepRunning(true);
		setConnectivity(false);
		
		// Start running
		thread = new Thread(this);
		thread.start();
	}
	
	public boolean getKeepRunning() {
		return keepRunning;
	}
	
	public void setKeepRunning(boolean b) {
		keepRunning = b;
	}
	
	public boolean getConnectivity() {
		return connectivity;
	}
	
	public void setConnectivity(boolean b) {
		this.connectivity = b;
	}
	
	// Network communication thread
	public void run() {
		while (!Thread.currentThread().isInterrupted() && getKeepRunning() && core.isWorldLoaded()) {
			try {
				// Starting the server (reuse & bind)
				serverSocket = new ServerSocket();
				serverSocket.setReuseAddress(true);
				serverSocket.bind(new InetSocketAddress(PORT));
				System.out.println("k9d3 Waiting for connection");
				
				// Waiting for a client to connect
				clientSocket = serverSocket.accept();
				setConnectivity(true);
				System.out.println("k9d3 Connection received from "+clientSocket.getInetAddress().getHostName());
				
				// IO setup
				out = new ObjectOutputStream(clientSocket.getOutputStream());
				out.flush();
	            in = new ObjectInputStream(clientSocket.getInputStream());
	            
	            sendMessage("Success!!");
	            
	            // The first time a client connects, you need to send everything
	            if (!Thread.currentThread().isInterrupted() && getKeepRunning() && core.isWorldLoaded() && getConnectivity() && clientSocket.isConnected()) {
	            	core.sendEverything();
	            }
	            
	            // TODO Listen for petitions
	            do {
	            	try {
	            		msg = (String) in.readObject();
	            		System.out.println("k9d3 client> "+msg);
	            		if (msg.equals("REMOTECRAFT_COMMAND_QUIT")) {
	            			sendMessage("REMOTECRAFT_COMMAND_QUIT");
	            		}
	            	} catch (ClassNotFoundException e) {
	            		e.printStackTrace();
	            	} catch (EOFException e) {
	            		e.printStackTrace();
	            		setConnectivity(false);
	            	}
	            //} while (clientSocket.isConnected() && core.isWorldLoaded() && !msg.equals("REMOTECRAFT_COMMAND_QUIT"));
	            } while (!Thread.currentThread().isInterrupted() && getKeepRunning() && core.isWorldLoaded() && getConnectivity() && clientSocket.isConnected() && !msg.equals("REMOTECRAFT_COMMAND_QUIT"));
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (out!=null)
						out.close();
					if (in != null)
						in.close();
					if (clientSocket != null)
						clientSocket.close();
					if (serverSocket != null)
						serverSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				setConnectivity(false);
			} // finally
		} // while
	} // run
	
	// Send message to the client
	public void sendMessage(String msg) {
		
		if (getKeepRunning() && core.isWorldLoaded() && getConnectivity() && clientSocket.isConnected()) {
			if (out != null) {
				try {
					out.writeObject(msg);
					out.flush();
					System.out.println("k9d3 server> "+msg);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	// Shutdown the Server
	public void shutdown() {
		
		sendMessage("REMOTECRAFT_COMMAND_QUIT");
		
		try {
			if (out!=null)
				out.close();
			
			if (in != null)
				in.close();
			
			if (clientSocket != null)
				clientSocket.close();
			
			if (serverSocket != null)
				serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		setConnectivity(false);
		
		setKeepRunning(false);
		
		thread.interrupt();
	}
	
	public void sendPlayername(String playerName) {
		sendMessage("REMOTECRAFT_INFO_PLAYERNAME:"+playerName);
	}
	
	public void sendHealth(int health) {
		sendMessage("REMOTECRAFT_INFO_HEALTH:"+health);
	}
	
	public void sendHunger(int hunger) {
		sendMessage("REMOTECRAFT_INFO_HUNGER:"+hunger);
	}
	
	public void sendArmor(int armor) {
		sendMessage("REMOTECRAFT_INFO_ARMOR:"+armor);
	}
	
	public void sendExpLevel(int expLvl) {
		sendMessage("REMOTECRAFT_INFO_EXPLVL:"+expLvl);
	}
	
	public void sendCoordX(int x) {
		String coordX = Integer.toString(x);
		sendMessage("REMOTECRAFT_INFO_COORDX:"+coordX);
	}
	
	public void sendCoordY(int y) {
		String coordY = Integer.toString(y);
		sendMessage("REMOTECRAFT_INFO_COORDY:"+coordY);
	}
	
	public void sendCoordZ(int z) {
		String coordZ = Integer.toString(z);
		sendMessage("REMOTECRAFT_INFO_COORDZ:"+coordZ);		
	}
	
	public void sendBiome(String biome) {
		sendMessage("REMOTECRAFT_INFO_BIOME:"+biome);
	}
	
	public void sendCurrentItem(String currentItem) {
		if (currentItem.equals("null")) {
			sendMessage("REMOTECRAFT_INFO_CURRENTITEM_NULL");
		} else {
			sendMessage("REMOTECRAFT_INFO_CURRENTITEM:"+currentItem);
		}
	}
	
	public void sendDaytime(boolean daytime) {
		if (daytime) {
			sendMessage("REMOTECRAFT_INFO_DAYTIME:TRUE");
		} else {
			sendMessage("REMOTECRAFT_INFO_DAYTIME:FALSE");
		}
	}
	
}
