package com.zireck.remotecraft;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.EnumSet;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.lwjgl.opengl.Drawable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ScreenShotHelper;
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
	String playerName = "";
	int health = 20;
	int hunger = 20;
	int armor = 0;
	int expLvl = 0;
	int x, y, z;
	String currentItem = "";
	
	// World info
	String worldName = "";
	long seed = 0;
	String biome = "";
	boolean isDaytime = true;
	boolean isRaining = false;
	boolean isThundering = false;
	int min = 0;
	int hour = 0;
	String timeZone = "am";
	
	long mWorldTime = 0;
	long mWorldTotalTime = 0;
	
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
		// Player Info
		updateCoords();
		updateBiome();
		updateExpLevel();
		updateHealth();
		updateHunger();
		updateArmor();
		updatePlayername();
		updateCurrentItem();
		
		// World Info
		updateWorldname();
		updateSeed();
		updateDaytime();
		updateTime();
		updateWeather();
	}
	
	public void sendEverything() {
		// Player Info
		nManager.sendPlayername(this.playerName);
		nManager.sendHealth(this.health);
		nManager.sendHunger(this.hunger);
		nManager.sendArmor(this.armor);
		nManager.sendCoordX(this.x);
		nManager.sendCoordY(this.y);
		nManager.sendCoordZ(this.z);
		nManager.sendBiome(this.biome);
		nManager.sendExpLevel(this.expLvl);
		nManager.sendCurrentItem(this.currentItem);
		
		// World Info
		nManager.sendWorldName(this.worldName);
		nManager.sendSeed(this.seed);
		nManager.sendDaytime(this.isDaytime);
		nManager.sendRaining(this.isRaining);
		nManager.sendThundering(this.isThundering);
		nManager.sendTime(this.hour, this.min, this.timeZone);
	}
	
	public void sendWorldInfo() {
		forceUpdateWorldname();
		updateSeed();
		forceUpdateDaytime();
		forceUpdateBiome();
		forceUpdateWeather();
		forceUpdateTime();
	}
	
	public void resetInfo() {
		this.x = Integer.MIN_VALUE;
		this.y = Integer.MIN_VALUE;
		this.z = Integer.MIN_VALUE;
		this.biome = "";
		this.expLvl = 0;
		this.health = 20;
		this.hunger = 20;
		this.armor = 0;
		this.playerName = "";
		this.currentItem = "";
		
		this.worldName = "";
		this.seed = 0;
		this.isDaytime = true;
		this.isRaining = false;
		this.isThundering = false;
		this.min = 0;
		this.hour = 0;
		this.timeZone = "am";
		
		this.mWorldTime = 0;
		this.mWorldTotalTime = 0;
	}
	
	public void updateWorldname() {
		IntegratedServer server = this.mc.getIntegratedServer();
		//String worldName = (server != null) ? server.getFolderName() : "sp_world";
		String worldName = (server != null) ? server.getWorldName() : "sp_world";
		if (this.worldName != worldName) {
			this.worldName = worldName;
			System.out.println("k9d3 worldname = "+this.worldName);
			nManager.sendWorldName(this.worldName);
		}
	}
	
	public void updateSeed() {
		if (this.seed != mc.getIntegratedServer().getServer().worldServers[0].getSeed()) {
			this.seed = mc.getIntegratedServer().getServer().worldServers[0].getSeed();
			nManager.sendSeed(this.seed);
		}
	}
	
	public void updatePlayername() {
		if (this.playerName != mc.thePlayer.getEntityName()) {
			this.playerName = mc.thePlayer.getEntityName();
			System.out.println("k9d3 username = "+this.playerName);
		}
	}
	
	public void updateHealth() {
		if (this.health != mc.thePlayer.getHealth()) {
			this.health = mc.thePlayer.getHealth();
			System.out.println("k9d3 current health = "+this.health);
			nManager.sendHealth(this.health);
		}
	}

	public void updateHunger() {
		if (this.hunger != mc.thePlayer.getFoodStats().getFoodLevel()) {
			this.hunger = mc.thePlayer.getFoodStats().getFoodLevel();
			System.out.println("k9d3 current hunger = "+this.hunger);
			nManager.sendHunger(this.hunger);
		}
	}
	
	public void updateArmor() {
		if (this.armor != mc.thePlayer.getTotalArmorValue()) {
			this.armor = mc.thePlayer.getTotalArmorValue();
			System.out.println("k9d3 current armor = "+this.armor);
			nManager.sendArmor(this.armor);
		}
	}
	
	public void updateExpLevel() {
		if (this.expLvl != mc.thePlayer.experienceLevel) {
			this.expLvl = mc.thePlayer.experienceLevel;
			System.out.println("k9d3 current exp level = "+this.expLvl);
			nManager.sendExpLevel(this.expLvl);
		}
	}
	
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
				
				// Take Screenshot
				//mc.ingameGUI.getChatGUI().printChatMessage("Screenshot tomada: "+ScreenShotHelper.saveScreenshot(mc.getMinecraftDir(), mc.displayWidth, mc.displayHeight));
				//String path = mc.getMinecraftDir() + File.separator + "screenshots" + File.separator + ScreenShotHelper.saveScreenshot(mc.getMinecraftDir(), mc.displayWidth, mc.displayHeight);
				//String path = mc.getAppDir("minecraft") + File.separator + "screenshots" + File.separator + ScreenShotHelper.saveScreenshot(mc.getMinecraftDir(), mc.displayWidth, mc.displayHeight);
				String path = "/Users/Zireck/Documents/forge/mcp/jars/screenshots/" + ScreenShotHelper.saveScreenshot(mc.getMinecraftDir(), mc.displayWidth, mc.displayHeight).split(" ")[3];
				nManager.sendScreenShot(path, this.worldName);
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}
	
	public void updateCurrentItem() {
		
		if (mc.thePlayer.inventory.getCurrentItem() == null && !this.currentItem.equals("")) {
			this.currentItem = "";
			nManager.sendCurrentItem("null");
		} else if (mc.thePlayer.inventory != null && mc.thePlayer.inventory.getCurrentItem() != null && mc.thePlayer.inventory.getCurrentItem().getDisplayName() != null) {
			
			if (!mc.thePlayer.inventory.getCurrentItem().getDisplayName().equals(this.currentItem)) {
				this.currentItem = mc.thePlayer.inventory.getCurrentItem().getDisplayName();
				nManager.sendCurrentItem(this.currentItem);
			}
			
		}
	}
	
	public void updateDaytime() {
		long time = mc.theWorld.getWorldTime() - 24000 * (int) (mc.theWorld.getWorldTime() / 24000);
		if (time >= 0 && time < 12000) {
			if (!this.isDaytime) {
				this.isDaytime = true;
				nManager.sendDaytime(isDaytime);
				System.out.println("k9d3 ahora DayTime es true");
			}
		} else {
			if (this.isDaytime) {
				this.isDaytime = false;
				nManager.sendDaytime(isDaytime);
				System.out.println("k9d3 ahora DayTime es false");
			}
		}
	}
	
	public void updateWeather() {
		if (!this.isRaining && mc.theWorld.getWorldInfo().isRaining()) {
			this.isRaining = true;
			nManager.sendRaining(this.isRaining);
			System.out.println("k9d3 started raining");
		} else if (this.isRaining && !mc.theWorld.getWorldInfo().isRaining()) {
			this.isRaining = false;
			nManager.sendRaining(this.isRaining);
			System.out.println("k9d3 stopped raining");
		}
		
		if (!this.isThundering && mc.theWorld.getWorldInfo().isThundering()) {
			this.isThundering = true;
			nManager.sendThundering(this.isThundering);
			System.out.println("k9d3 started thundering");
		} else if (this.isThundering && !mc.theWorld.getWorldInfo().isThundering()) {
			this.isThundering = false;
			nManager.sendThundering(this.isThundering);
			System.out.println("k9d3 stopped thundering");
		}
	}
	
	public void updateTime() {
		// Time
		long time;
		if (mc.theWorld.getWorldTime() > 24000) {
			time = mc.theWorld.getWorldTime() - 24000 * (int) (mc.theWorld.getWorldTime() / 24000);
		} else {
			time = mc.theWorld.getWorldTime();
		}
		
		int hour, minute;
		String timeZ = "";
		
		// Calculate minutes
		if ( ((time * 60) / 1000) > 60 ) {
			minute = (int) ((time * 60) / 1000) - (60 * ((int) time / 1000));
		} else {
			minute = (int) ((time * 60) / 1000);
		}
		
		// if minutes == 0 or 15 or 30 or 45, then update and send
		if (minute == 0 || minute == 15 || minute == 30 || minute == 45) {
			
			// Calculate hour
			if ( (((int) time / 1000) + 6) > 23 ) {
				hour = (((int) time / 1000) + 6) - 24;
			} else {
				hour = ((int) time / 1000) + 6;
			}
			
			// Set hour from 24 to 12
			if (hour > 11) {
				hour = hour - 12;
				timeZ = "pm";
			} else {
				timeZ = "am";
			}
			
			// Set 0 to 12
			if (hour == 0) {
				hour = 12;
			}
			
			if (this.hour != hour || this.min != minute || !this.timeZone.equals(timeZ)) {
				this.hour = hour;
				this.min = minute;
				this.timeZone = timeZ;
				nManager.sendTime(this.hour, this.min, this.timeZone);
				System.out.println("k9d3 Time: "+hour+":"+minute);
			}
			
		}

	}
	
	public void forceUpdateWorldname() {
		IntegratedServer server = this.mc.getIntegratedServer();
		//String worldName = (server != null) ? server.getFolderName() : "sp_world";
		String worldName = (server != null) ? server.getWorldName() : "sp_world";
		this.worldName = worldName;
		nManager.sendWorldName(this.worldName);
		System.out.println("k9d3 worldname = "+this.worldName);
	}
	
	public void forceUpdateDaytime() {
		long time = mc.theWorld.getWorldTime() - 24000 * (int) (mc.theWorld.getWorldTime() / 24000);
		if (time >= 0 && time < 12000) {
			this.isDaytime = true;
			nManager.sendDaytime(isDaytime);
			System.out.println("k9d3 ahora DayTime es true");
		} else {
			this.isDaytime = false;
			nManager.sendDaytime(isDaytime);
			System.out.println("k9d3 ahora DayTime es false");
		}
	}
	
	public void forceUpdateBiome() {
		String biome = "";
		int x = MathHelper.floor_double(mc.thePlayer.posX);
		int z = MathHelper.floor_double(mc.thePlayer.posZ);
		Chunk chunk = mc.theWorld.getChunkFromBlockCoords(x, z);
		biome = chunk.getBiomeGenForWorldCoords(x & 15, z & 15, mc.theWorld.getWorldChunkManager()).biomeName;
		
		try {
			this.biome = biome;
			nManager.sendBiome(this.biome);
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}
	
	public void forceUpdateWeather() {
		if (mc.theWorld.getWorldInfo().isRaining()) {
			this.isRaining = true;
			nManager.sendRaining(this.isRaining);
			System.out.println("k9d3 started raining");
		} else {
			this.isRaining = false;
			nManager.sendRaining(this.isRaining);
			System.out.println("k9d3 stopped raining");
		}
		
		if (mc.theWorld.getWorldInfo().isThundering()) {
			this.isThundering = true;
			nManager.sendThundering(this.isThundering);
			System.out.println("k9d3 started thundering");
		} else {
			this.isThundering = false;
			nManager.sendThundering(this.isThundering);
			System.out.println("k9d3 stopped thundering");
		}
	}
	
	public void forceUpdateTime() {
		long time;
		if (mc.theWorld.getWorldTime() > 24000) {
			time = mc.theWorld.getWorldTime() - 24000 * (int) (mc.theWorld.getWorldTime() / 24000);
		} else {
			time = mc.theWorld.getWorldTime();
		}
		
		int hour, minute;
		// Calculate hour
		if ( (((int) time / 1000) + 6) > 23 ) {
			hour = (((int) time / 1000) + 6) - 24;
		} else {
			hour = ((int) time / 1000) + 6;
		}

		// Calculate minutes
		if ( ((time * 60) / 1000) > 60 ) {
			minute = (int) ((time * 60) / 1000) - (60 * ((int) time / 1000));
		} else {
			minute = (int) ((time * 60) / 1000);
		}
		this.min = minute;
		
		// Set hour from 24 to 12
		if (hour > 11) {
			this.hour = hour - 12;
			this.timeZone = "pm";
		} else {
			this.hour = hour;
			this.timeZone = "am";
		}
		
		// Set 0 to 12
		if (this.hour == 0) {
			this.hour = 12;
		}
		
		System.out.println("k9d3 Time: "+hour+":"+minute);
		
		nManager.sendTime(this.hour, this.min, this.timeZone);
		
	}
	
	public void updateGameMode() {
		/*
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
		}*/
	}
	
	public void setHealth(String health) {
		int mHealth = Integer.parseInt(health);
		mc.getIntegratedServer().getServer().worldServers[0].getPlayerEntityByName(this.playerName).setEntityHealth(mHealth);
	}
	
	public void setHunger(String hunger) {
		int mHunger = Integer.parseInt(hunger);
		mc.getIntegratedServer().worldServers[0].getPlayerEntityByName(mc.thePlayer.username).getFoodStats().setFoodLevel(mHunger);
	}
	
	public void setExpLvl(String xpLvl) {
		int mXpLvl = Integer.parseInt(xpLvl);
		mc.getIntegratedServer().worldServers[0].getPlayerEntityByName(mc.thePlayer.username).addExperienceLevel(0);
		mc.getIntegratedServer().worldServers[0].getPlayerEntityByName(mc.thePlayer.username).addExperienceLevel(mXpLvl);
	}
	
	public void toggleGameMode() {
		if (mc.playerController.isInCreativeMode()) {
			mc.getIntegratedServer().getServer().worldServers[0].getPlayerEntityByName(mc.thePlayer.username).setGameType(EnumGameType.SURVIVAL);
		} else {
			mc.getIntegratedServer().getServer().worldServers[0].getPlayerEntityByName(mc.thePlayer.username).setGameType(EnumGameType.CREATIVE);
		}
	}
	
	public void setWorldTime(String dayOrNight) {
		if (dayOrNight.equals("DAY")) {
			//mc.theWorld.setWorldTime(0);
			System.out.println("k9d3 Trying to set Time as Day");
			mc.getIntegratedServer().getServer().worldServers[0].setWorldTime(0);
		} else if (dayOrNight.equals("NIGHT")) {
			//mc.theWorld.setWorldTime(12500);
			System.out.println("k9d3 Trying to set Time as Night");
			mc.getIntegratedServer().getServer().worldServers[0].setWorldTime(12500);
		}
	}
	
	public void setWorldWeather() {
		System.out.println("k9d3 TOGGLE RAIN ()");
		//mc.theWorld.toggleRain();
		mc.getIntegratedServer().getServer().worldServers[0].toggleRain();
	}
	
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
