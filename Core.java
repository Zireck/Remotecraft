package com.zireck.remotecraft;

import java.util.EnumSet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.FoodStats;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StringUtils;
import net.minecraft.world.EnumGameType;
import net.minecraft.world.chunk.Chunk;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;

public class Core implements ITickHandler {
	
	Minecraft mc = Minecraft.getMinecraft();
	GuiScreen guiScreen = Minecraft.getMinecraft().currentScreen;
	
	// Tells you if there's a minecraft world loaded
	boolean isWorldLoaded = false;

	// Socket manager
	NetworkManager nManager = null;
	
	// Discovery Socket
	NetworkDiscovery nDiscovery = null;
	Thread nDiscoveryThread = null;
	
	// User info
	String username = "";
	int health = 20;
	int hunger = 20;
	int expLvl = 0;
	int x, y, z;
	
	// World info
	String worldName = "";
	String biome = "";
	boolean isRaining = false;
	boolean isThundering = false;
	
	@Override
	public void tickStart(EnumSet<TickType> type, Object... tickData) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void tickEnd(EnumSet<TickType> type, Object... tickData) {
		// TODO Auto-generated method stub
		
		if (mc.theWorld == null) {
			onTickInGUI(mc.currentScreen);
		} else {
			onTickInGame();
		}
		
	}

	@Override
	public EnumSet<TickType> ticks() {
		// TODO Auto-generated method stub
		return EnumSet.of(TickType.CLIENT, TickType.CLIENT);
	}

	@Override
	public String getLabel() {
		// TODO Auto-generated method stub
		return "Core";
	}

	public void onTickInGame() {
		if (!isWorldLoaded()) {
			
			setWorldLoaded();
			
			// Network Discovery
			nDiscovery = NetworkDiscovery.getInstance();
			nDiscovery.init(worldName);

			nDiscoveryThread = new Thread(nDiscovery);
			nDiscoveryThread.start();
			
			// Network Manager
			nManager = new NetworkManager(this);
			
		}
		
		updateInfo();
		
		if (nDiscovery.worldName == "") {
			nDiscovery.worldName = worldName;
		}
	}
	
	public void onTickInGUI(GuiScreen guiScreen) {
		if (isWorldLoaded()) {
			setWorldUnloaded();
			
			// Shutdown the Network Discovery thread
			if (nDiscoveryThread != null) {
				nDiscoveryThread.interrupt();
				nDiscoveryThread = null;
			}
			
			// Shutdown the Network Manager
			if (nManager != null) {
				nManager.shutdown();
				nManager = null;
			}
			
			resetInfo();
		}
	}
	
	public boolean isWorldLoaded() {
		return isWorldLoaded;
	}
	
	public void setWorldLoaded() {
		isWorldLoaded = true;
	}
	
	public void setWorldUnloaded() {
		isWorldLoaded = false;
	}
	
	public void updateInfo() {
		updateCoords();
		updateBiome();
		updateExpLevel();
		updateHealth();
		updateHunger();
		updateUsername();
		
		updateWorldname();
		updateWeather();
		//updateGameMode();
	}
	
	public void sendEverything() {
		nManager.sendCoordX(this.x);
		nManager.sendCoordY(this.y);
		nManager.sendCoordZ(this.z);
		nManager.sendBiome(this.biome);
	}
	
	public void resetInfo() {
		this.x = Integer.MIN_VALUE;
		this.y = Integer.MIN_VALUE;
		this.z = Integer.MIN_VALUE;
		this.biome = "";
		this.expLvl = 0;
		this.health = 20;
		this.hunger = 20;
		this.username = "";
		
		this.worldName = "";
		this.isRaining = false;
		this.isThundering = false;
	}
	
	// Update coords X, Y, Z
	public void updateCoords() {
		int x = MathHelper.floor_double(mc.thePlayer.posX);
		int y = MathHelper.floor_double(mc.thePlayer.posY);
		int z = MathHelper.floor_double(mc.thePlayer.posZ);
		
		if (this.x != x) {
			this.x = x;
			System.out.println("k9d3 x coord = "+this.x);
			nManager.sendCoordX(this.x);
		}
		
		if (this.y != y) {
			this.y = y;
			System.out.println("k9d3 y coord = "+this.y);
			nManager.sendCoordY(this.y);
		}
		
		if (this.z != z) {
			this.z = z;
			System.out.println("k9d3 z coord = "+this.z);
			nManager.sendCoordZ(this.z);
		}
		
	}
	
	public void updateBiome() {
		String biome = "";
		int x = MathHelper.floor_double(mc.thePlayer.posX);
		int z = MathHelper.floor_double(mc.thePlayer.posZ);
		Chunk chunk = mc.theWorld.getChunkFromBlockCoords(x, z);
		biome = chunk.getBiomeGenForWorldCoords(x & 15, z & 15, mc.theWorld.getWorldChunkManager()).biomeName;
		
		try {
			if (!this.biome.equals(biome)) {
				System.out.println("k9d3 previous biome: "+this.biome);
				System.out.println("k9d3 new biome: "+biome);
				this.biome = biome;
				nManager.sendBiome(this.biome);
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}
	
	public void updateExpLevel() {
		if (this.expLvl != mc.thePlayer.experienceLevel) {
			this.expLvl = mc.thePlayer.experienceLevel;
			System.out.println("k9d3 current exp level = "+this.expLvl);
		}
	}
	
	public void updateHealth() {
		if (this.health != mc.thePlayer.getHealth()) {
			this.health = mc.thePlayer.getHealth();
			System.out.println("k9d3 current health = "+this.health);
		}
	}

	public void updateHunger() {
		if (this.hunger != mc.thePlayer.getFoodStats().getFoodLevel()) {
			this.hunger = mc.thePlayer.getFoodStats().getFoodLevel();
			System.out.println("k9d3 current hunger = "+this.hunger);
		}
	}
	
	public void updateUsername() {
		if (this.username != mc.thePlayer.getEntityName()) {
			this.username = mc.thePlayer.getEntityName();
			System.out.println("k9d3 username = "+this.username);
		}
	}
	
	public void updateWorldname() {
		IntegratedServer server = this.mc.getIntegratedServer();
		//String worldName = (server != null) ? server.getFolderName() : "sp_world";
		String worldName = (server != null) ? server.getWorldName() : "sp_world";
		if (this.worldName != worldName) {
			this.worldName = worldName;
			System.out.println("k9d3 worldname = "+this.worldName);
		}
	}
	
	public void updateWeather() {
		if (!this.isRaining && mc.theWorld.getWorldInfo().isRaining()) {
			this.isRaining = true;
			System.out.println("k9d3 started raining");
		} else if (this.isRaining && !mc.theWorld.getWorldInfo().isRaining()) {
			this.isRaining = false;
			System.out.println("k9d3 stopped raining");
		}
		
		if (!this.isThundering && mc.theWorld.getWorldInfo().isThundering()) {
			this.isThundering = true;
			System.out.println("k9d3 started thundering");
		} else if (this.isThundering && !mc.theWorld.getWorldInfo().isThundering()) {
			this.isThundering = false;
			System.out.println("k9d3 stopped thundering");
		}
	}
	
	/*public void updateGameMode() {
		//EnumGameType currentGameMode = mc.theWorld.getWorldInfo().getGameType();
		EnumGameType currentGameMode = mc.getIntegratedServer().getGameType();
		
		if (!this.gameMode.equals("C") && currentGameMode == EnumGameType.CREATIVE) {
			this.gameMode = "C";
			System.out.println("k9d3 new game mode: creative");
		}
		
		if (!this.gameMode.equals("A") && currentGameMode.isAdventure()) {
			this.gameMode = "A";
			System.out.println("k9d3 new game mode: adventure");			
		}
		
		if (!currentGameMode.isAdventure() && currentGameMode.isSurvivalOrAdventure()) {
			// it's survival
			if (!this.gameMode.equals("S")) {
				this.gameMode = "S";
				System.out.println("k9d3 new game mode: survival");
			}
		}
	}*/
	
	public static boolean isWorldMultiplayer() {
		try {
			if( MinecraftServer.getServer().isServerRunning() ) {
				return !MinecraftServer.getServer().isSinglePlayer();
			}
			return true;
		} catch (Exception e) {
			return true;
		}
	}
	
	public static boolean isWorldSinglePlayer() {
		try {
			if (MinecraftServer.getServer().isServerRunning()) {
				return MinecraftServer.getServer().isSinglePlayer();
			}
			return false;
		} catch (Exception e) { // Server is null, not started
			return false;
		}
	}
}
