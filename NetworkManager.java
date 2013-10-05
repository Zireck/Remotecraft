package com.zireck.remotecraft;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

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
	            		if (msg.equals("REMOTECRAFT_COMMAND_GETWORLDINFO")) {
	            			core.sendWorldInfo();
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
	
	// *** WORLD INFO *** //
	public void sendWorldName(String worldName) {
		sendMessage("REMOTECRAFT_INFO_WORLDNAME:"+worldName);
	}
	
	public void sendDaytime(boolean daytime) {
		if (daytime) {
			sendMessage("REMOTECRAFT_INFO_DAYTIME:TRUE");
		} else {
			sendMessage("REMOTECRAFT_INFO_DAYTIME:FALSE");
		}
	}
	
	public void sendTime(int hour, int minute, String timeZone) {
		String mHour = Integer.toString(hour);
		String mMin;
		if (minute < 10) {
			mMin = "0"+Integer.toString(minute);
		} else {
			mMin = Integer.toString(minute);
		}
		String mTime = mHour + "_" + mMin + "_" + timeZone;
		sendMessage("REMOTECRAFT_INFO_TIME:"+mTime);
	}
	
	public void sendRaining(boolean isRaining) {
		if (isRaining) {
			sendMessage("REMOTECRAFT_INFO_RAINING:TRUE");
		} else {
			sendMessage("REMOTECRAFT_INFO_RAINING:FALSE");
		}
	}
	
	public void sendThundering(boolean isThundering) {
		if (isThundering) {
			sendMessage("REMOTECRAFT_INFO_THUNDERING:TRUE");
		} else {
			sendMessage("REMOTECRAFT_INFO_THUNDERING:FALSE");
		}
	}
	
	public void sendScreenShot(String fileName) {
		
		sendMessage("REMOTECRAFT_COMMAND_SCREENSHOT_SEND");
		
		// File to send
		File myFile = new File(fileName);
		int fSize = (int) myFile.length();
		if (fSize < myFile.length()) {
			System.out.println("File is too big");
			//throw new IOException("File is too big");
		}
		
		// Send the file's size
		byte[] bSize = new byte[4];
		bSize[0] = (byte) ((fSize & 0xff000000) >> 24);
	    bSize[1] = (byte) ((fSize & 0x00ff0000) >> 16);
	    bSize[2] = (byte) ((fSize & 0x0000ff00) >> 8);
	    bSize[3] = (byte) (fSize & 0x000000ff);
	    // 4 bytes containing the file size
	    try {
	    	out.write(bSize, 0, 4);
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }
	    
	    FileInputStream fis = null;
	    BufferedInputStream bis = null;
	    try {
	    	fis = new FileInputStream(myFile);
	    	bis = new BufferedInputStream(fis);
	    
	    	byte[] outBuffer = new byte[fSize];
	    	int bRead = bis.read(outBuffer, 0, outBuffer.length);
	    	out.write(outBuffer, 0, bRead);
	    	out.flush();
	    } catch (IOException e) {
	    	e.printStackTrace();
	    } finally {
	    	try {
	    		bis.close();
	    	} catch (IOException e) {
	    		e.printStackTrace();
	    	}
	    }
	    
	    sendMessage("REMOTECRAFT_COMMAND_SCREENSHOT_FINISHED");
		
	}
	
	/*
	public void sendScreenShot(Image screenShot) {
		if (getKeepRunning() && core.isWorldLoaded() && getConnectivity() && clientSocket.isConnected()) {
			if (out != null) {
				try {
					out.writeObject(screenShot);
					out.flush();
					System.out.println("k9d3 screenShot enviada!");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}*/
}
