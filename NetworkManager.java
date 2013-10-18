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
import java.net.SocketException;

import net.minecraft.client.Minecraft;

public class NetworkManager implements Runnable {
	
	private final int MANAGER_PORT = 9999;
	
	// Server Socket
	private ServerSocket serverSocket;
	private Socket clientSocket = null;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private String msg;
	
	// Self Thread
	private Thread thread;

	// This class needs to stop running
	private boolean keepRunning;
	
	// Tells you if there's a currently established socket connection
	private boolean connectivity;
	
	// Reference to Core class
	private INetworkListener mCallback;
	
	public interface INetworkListener {
		public boolean isWorldLoaded();
		
		public void sendEverything();
		
		public void setHealth(int mHealth);
		public void setHunger(int mHunger);
		public void setExpLvl(int mExpLvl);
		public void toggleGameMode();

		public void setWorldTime(String dayOrNight);
		public void setWorldWeather();
		public void enableScreenShot();
		public void teleportTo(int mDim, int x, int y, int z);
		public void toggleButton(int mDim, int x, int y, int z);
		public void toggleLever(int mDim, int x, int y, int z);
		
		public void forceSendWorldInfo();
	}
	
	public NetworkManager(Core core) {
		// Make sure that Core class implements INetworkListener
		try {
			mCallback = (INetworkListener) core;
		} catch (ClassCastException e) {
			e.printStackTrace();
		}
		
		setKeepRunning(true);
		setConnectivity(false);
		
		// Start running thread
		thread = new Thread(this);
		thread.start();
	}
	
	private boolean getKeepRunning() {
		return keepRunning;
	}
	
	private void setKeepRunning(boolean b) {
		keepRunning = b;
	}
	
	private boolean getConnectivity() {
		return connectivity;
	}
	
	private void setConnectivity(boolean b) {
		connectivity = b;
	}
	
	// Shutdown the Server, kind of like a class destructor
	public void shutdown() {
		sendMessage("REMOTECRAFT_COMMAND_QUIT");
		
		setConnectivity(false);
		setKeepRunning(false);
		
		thread.interrupt();
		
		try {

			if (out!=null) {
				synchronized (out) {
					out.close();
				}
			}
			
			if (in != null)
				in.close();
			
			if (clientSocket != null)
				clientSocket.close();
			
			if (serverSocket != null)
				serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		thread = null;
		
	}
	
	// Network communication thread
	public void run() {

		while (!Thread.currentThread().isInterrupted() && getKeepRunning()) {
		
			try {
				// Starting the server (reuse & bind)
				serverSocket = new ServerSocket();
				serverSocket.setReuseAddress(true);
				serverSocket.bind(new InetSocketAddress(MANAGER_PORT));
				
				// Waiting for a client to connect
				clientSocket = serverSocket.accept();
				setConnectivity(true);
				
				// IO setup
				in = new ObjectInputStream(clientSocket.getInputStream());
				out = new ObjectOutputStream(clientSocket.getOutputStream());
				out.flush();
	            
	            // The first time a client is connected, you need to send him everything over the socket
	            if (!thread.isInterrupted() && getKeepRunning() && getConnectivity()) {
	            	mCallback.sendEverything();
	            }
	            
	            // Socket communication main loop
	            while (!Thread.currentThread().isInterrupted() && getKeepRunning() && getConnectivity()) {
	            	try {
	            		msg = (String) in.readObject();
	            		processMessage(msg);
	            	} catch (ClassNotFoundException e) {
	            		e.printStackTrace();
	            	} catch (EOFException e) {
	            		e.printStackTrace();
	            		setConnectivity(false);
	            	} catch (SocketException e) {
	            		e.printStackTrace();
	            		setConnectivity(false);
	            	}
				}
	            
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if (out!=null) {
						synchronized (out) {
							out.close();
							out = null;
						}
					}
					
					if (in != null) {
						in.close();
						in = null;
					}
					
					if (clientSocket != null) {
						clientSocket.close();
						clientSocket = null;
					}
					
					if (serverSocket != null) {
						serverSocket.close();
						serverSocket = null;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				setConnectivity(false);
			} // finally
		} // while
	} // run
	
	// Send message to the client
	public void sendMessage(String msg) {
		
		if (getKeepRunning() && getConnectivity()) {
			if (out != null) {
				synchronized (out) {
					try {
						out.writeObject(msg);
						out.flush();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
	}
	
	private void processMessage(String msg) {
		
		if (msg.equals("REMOTECRAFT_COMMAND_QUIT")) {
			//sendMessage("REMOTECRAFT_COMMAND_QUIT");
			setConnectivity(false);
		} else {
		
			// Single parameter
			// Everytime the WorldFragment is created in the Android app
			if (msg.equals("REMOTECRAFT_COMMAND_GETWORLDINFO")) {
				mCallback.forceSendWorldInfo();
			} else if (msg.equals("REMOTECRAFT_COMMAND_SETWEATHER")) {
				mCallback.setWorldWeather();
			} else if (msg.equals("REMOTECRAFT_COMMAND_SETGAMEMODE")) {
				mCallback.toggleGameMode();
			} else if (msg.equals("REMOTECRAFT_COMMAND_GETSCREENSHOT")) {
				mCallback.enableScreenShot();
			} 

			// More than 1 parameter
			String mCommand = msg.split(":")[0];
			if (mCommand.equals("REMOTECRAFT_COMMAND_SETTIME")) {
				String dayOrNight = msg.split(":")[1];
				mCallback.setWorldTime(dayOrNight);
			} else if (mCommand.equals("REMOTECRAFT_COMMAND_SETHEALTH")) {
				int mHealth = Integer.parseInt(msg.split(":")[1]);
				mCallback.setHealth(mHealth);
			} else if (mCommand.equals("REMOTECRAFT_COMMAND_SETHUNGER")) {
				int mHunger = Integer.parseInt(msg.split(":")[1]);
				mCallback.setHunger(mHunger);
			} else if (mCommand.equals("REMOTECRAFT_COMMAND_SETEXPLVL")) {
				int mExpLvl = Integer.parseInt(msg.split(":")[1]);
				mCallback.setExpLvl(mExpLvl);
			} else if (mCommand.equals("REMOTECRAFT_COMMAND_TELEPORT")) {
				String mLocation = msg.split(":")[1];
				String dimension = mLocation.split("_")[0];
				int mDim;
				if (dimension.equalsIgnoreCase("Nether")) {
					mDim = -1;
				} else if (dimension.equalsIgnoreCase("End")) {
					mDim = 1;
				} else {
					mDim = 0;
				}
				int x = Integer.parseInt(mLocation.split("_")[1]);
				int y = Integer.parseInt(mLocation.split("_")[2]);
				int z = Integer.parseInt(mLocation.split("_")[3]);
				mCallback.teleportTo(mDim, x, y, z);
			} else if (mCommand.equals("REMOTECRAFT_COMMAND_REDSTONE_BUTTON")) {
				String mLocation = msg.split(":")[1];
				String dimension = mLocation.split("_")[0];
				int mDim;
				if (dimension.equalsIgnoreCase("Nether")) {
					mDim = -1;
				} else if (dimension.equalsIgnoreCase("End")) {
					mDim = 1;
				} else {
					mDim = 0;
				}
				int x = Integer.parseInt(mLocation.split("_")[1]);
				int y = Integer.parseInt(mLocation.split("_")[2]);
				int z = Integer.parseInt(mLocation.split("_")[3]);
				mCallback.toggleButton(mDim, x, y, z);
			} else if (mCommand.equals("REMOTECRAFT_COMMAND_REDSTONE_LEVER")) {
				String mLocation = msg.split(":")[1];
				String dimension = mLocation.split("_")[0];
				int mDim = 0;
				if (dimension.equalsIgnoreCase("Nether")) {
					mDim = -1;
				} else if (dimension.equalsIgnoreCase("End")) {
					mDim = 1;
				} else {
					mDim = 0;
				}
				int x = Integer.parseInt(mLocation.split("_")[1]);
				int y = Integer.parseInt(mLocation.split("_")[2]);
				int z = Integer.parseInt(mLocation.split("_")[3]);
				mCallback.toggleLever(mDim, x, y, z);
			}
		
		}
	}
	
	// Player Info
	
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
	
	// World Info
	
	public void sendWorldName(String worldName) {
		sendMessage("REMOTECRAFT_INFO_WORLDNAME:"+worldName);
	}
	
	public void sendSeed(long seed) {
		sendMessage("REMOTECRAFT_INFO_SEED:"+Long.toString(seed));
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
	
	public void sendScreenShot(String fileName, long mSeed) throws IOException {
		
		synchronized (out) {
			
			sendMessage("REMOTECRAFT_COMMAND_SCREENSHOT_SEND:"+String.valueOf(mSeed));
			
			// File to send
			File myFile = new File(Minecraft.getMinecraftDir().getCanonicalPath() + File.separator + "screenshots" + File.separator, fileName);
			int fSize = (int) myFile.length();
			if (fSize < myFile.length()) {
				System.out.println("File is too big");
				sendMessage("REMOTECRAFT_COMMAND_SCREENSHOT_ERROR");
				return;
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
		    	sendMessage("REMOTECRAFT_COMMAND_SCREENSHOT_ERROR");
		    	return;
		    }
		    
		    boolean noMemoryLimitation = false;
		    
		    FileInputStream fis = null;
		    BufferedInputStream bis = null;
		    try {
		    	fis = new FileInputStream(myFile);
		    	bis = new BufferedInputStream(fis);
		    	
		    	if (noMemoryLimitation) {
		    
			    	byte[] outBuffer = new byte[fSize];
			    	int bRead = bis.read(outBuffer, 0, outBuffer.length);
			    	out.write(outBuffer, 0, bRead);
		    	
		    	} else {
		    		
		    		int bRead = 0;
		    		byte[] outBuffer = new byte[8*1024];
		    		while ( (bRead = bis.read(outBuffer, 0, outBuffer.length)) > 0 ) {
		    			out.write(outBuffer, 0, bRead);
		    		}
		    		
		    	}
		    	out.flush();
		    } catch (IOException e) {
		    	e.printStackTrace();
		    	sendMessage("REMOTECRAFT_COMMAND_SCREENSHOT_ERROR");
		    	return;
		    } finally {
		    	try {
		    		bis.close();
		    	} catch (IOException e) {
		    		e.printStackTrace();
		    	} catch (NullPointerException e) {
		    		e.printStackTrace();
		    	}
		    	sendMessage("REMOTECRAFT_COMMAND_SCREENSHOT_FINISHED:"+String.valueOf(mSeed));
		    }
		    
		}
		
	}

}
