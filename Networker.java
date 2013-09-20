package com.zireck.remotecraft;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class Networker implements Runnable {
	
	private final int PORT = 4444;
	
	Core core;
	
	ServerSocket serverSocket;
	Socket clientSocket = null;
	ObjectOutputStream out;
	ObjectInputStream in;
	String msg;
	
	// Tells you if there's a currently established socket connection
	Boolean connectivity;

	public Networker(Core core) {
		// TODO Auto-generated constructor stub
		
		this.core = core;
		
		connectivity = false;
		
		Thread t = new Thread(this);
		t.start();
		
	}
	
	public void run() {
		while (true && core.isWorldLoaded()) {
			try {
				// Starting the server (reuse & bind)
				serverSocket = new ServerSocket();
				serverSocket.setReuseAddress(true);
				serverSocket.bind(new InetSocketAddress(PORT));
				System.out.println("k9d3 Waiting for connection");
				
				// Waiting for a client to connect
				clientSocket = serverSocket.accept();
				connectivity = true;
				System.out.println("k9d3 connection received from "+clientSocket.getInetAddress().getHostName());
				
				// streams setup
				out = new ObjectOutputStream(clientSocket.getOutputStream());
	            out.flush();
	            in = new ObjectInputStream(clientSocket.getInputStream());
	            
	            sendMessage("Conectado correctamente!!");
	            
	            // Send everything
	            if (clientSocket.isConnected() && core.isWorldLoaded()) {
	            	core.sendEverything();
	            }
	            
	            // TODO: Listen for petitions
	            do {
	            	try {
	            		msg = (String) in.readObject();
	            		System.out.println("k9d3 client> "+msg);
	            		if (msg.equals("bye")) {
	            			sendMessage("bye");
	            		}
	            	} catch (ClassNotFoundException e) {
	            		e.printStackTrace();
	            	}
	            } while (clientSocket.isConnected() && core.isWorldLoaded() && !msg.equals("bye"));
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					// Closing socket
					connectivity = false;
					in.close();
					out.close();
					clientSocket.close();
					serverSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void shutdown() {
		try {
			sendMessage("quit");
			out.close();
			in.close();
			clientSocket.close();
			serverSocket.close();
			connectivity = false;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void sendMessage(String m) {
		try {
			if (out!=null) {
				out.writeObject(m);
				out.flush();
				System.out.println("k9d3 server> "+m);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}
	
	public boolean connectivity() {
		return connectivity;
	}
	
	public void sendCoordX(int x) {
		if (connectivity()) {
			String coordX = Integer.toString(x);
			if (out!=null) {
				try {
					out.writeObject("coordX:"+coordX);
				} catch (IOException ioException) {
					ioException.printStackTrace();
				}
			}
		}
	}
	
	public void sendCoordY(int y) {
		if (connectivity()) {
			String coordY = Integer.toString(y);
			if (out!=null) {
				try {
					out.writeObject("coordY:"+coordY);
				} catch (IOException ioException) {
					ioException.printStackTrace();
				}
			}
		}
	}
	
	public void sendCoordZ(int z) {
		if (connectivity()) {
			String coordZ = Integer.toString(z);
			if (out!=null) {
				try {
					out.writeObject("coordZ:"+coordZ);
				} catch (IOException ioException) {
					ioException.printStackTrace();
				}
			}
		}
	}
	
	public void sendBiome(String biome) {
		if (connectivity()) {
			if (out!=null) {
				try {
					out.writeObject("biome:"+biome);
				} catch (IOException ioException) {
					ioException.printStackTrace();
				}
			}
		}
	}
	
}
