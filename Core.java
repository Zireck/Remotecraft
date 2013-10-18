package com.zireck.remotecraft;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Random;

import com.zireck.remotecraft.NetworkManager.INetworkListener;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.StringUtils;
import net.minecraft.world.EnumGameType;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;

public class Core implements ITickHandler, INetworkListener {
	
	Minecraft mc;
	
	// Tells you if there's a minecraft world loaded
	private boolean isWorldLoaded;

	// Socket manager
	NetworkManager nManager;
	
	// Discovery Socket
	NetworkDiscovery nDiscovery;
	//Thread nDiscoveryThread;
	
	// User info
	String playerName;
	int health;
	int hunger;
	int armor;
	int expLvl;
	int x, y, z;
	String biome;
	String currentItem;
	
	// World info
	String worldName;
	long seed;
	boolean isDaytime;
	int hour;
	int minute;
	String timeZone;
	boolean isRaining;
	boolean isThundering;
	
	boolean shouldTakeScreenShot;
	boolean shouldIGoToNether;
	
	public Core() {
		// TODO Auto-generated constructor stub
		mc = Minecraft.getMinecraft();
		
		isWorldLoaded = false;
		nManager = null;
		nDiscovery = null;
		//nDiscoveryThread = null;
		
		resetInfo();
	}
	
	@Override
	public void tickStart(EnumSet<TickType> type, Object... tickData) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void tickEnd(EnumSet<TickType> type, Object... tickData) {
		// TODO Auto-generated method stub
		
		if (mc != null) {
			if (mc.theWorld != null) {
				onTickInGame();				
			} else {
				onTickInGUI();
			}
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

	private void onTickInGame() {
		
		if (!isWorldLoaded()) {
			setWorldLoaded();
			
			// Network Discovery
			nDiscovery = NetworkDiscovery.getInstance();
			nDiscovery.setWorldName(worldName);

			//nDiscoveryThread = new Thread(nDiscovery);
			//nDiscoveryThread.start();
			
			// Network Manager
			nManager = new NetworkManager(this);
		}
		
		updateInfo();
		
		if (nDiscovery.getWorldName().equals("") && !this.worldName.equals("")) {
			//nDiscovery.worldName = this.worldName;
			nDiscovery.setWorldName(worldName);
		}
		
	}
	
	private void onTickInGUI() {
		if (isWorldLoaded()) {
			setWorldUnloaded();
			
			// Shutdown the Network Discovery thread
			/*if (nDiscoveryThread != null) {
				nDiscoveryThread.interrupt();
				nDiscoveryThread = null;
			}*/
			if (nDiscovery != null) {
				nDiscovery.shutdown();
				nDiscovery = null;
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
	
	// Called every game tick. Update info and send when necessary
	public void updateInfo() {
		updatePlayername();
		updateHealth();
		updateHunger();
		updateExpLevel();
		updateArmor();
		updateCoords();
		updateBiome();
		updateCurrentItem();
		
		updateWorldname();
		updateSeed();
		updateDaytime();
		updateTime();
		updateWeather();
		
		getScreenshot();
	}
	
	// Called every GUI tick, initialize data
	public void resetInfo() {
		this.playerName = "";
		this.health = Integer.MIN_VALUE;
		this.hunger = Integer.MIN_VALUE;
		this.armor = Integer.MIN_VALUE;
		this.expLvl = Integer.MIN_VALUE;
		this.x = Integer.MIN_VALUE;
		this.y = Integer.MIN_VALUE;
		this.z = Integer.MIN_VALUE;
		this.biome = "";
		this.currentItem = "";
		
		this.worldName = "";
		this.seed = Long.MIN_VALUE;
		this.isDaytime = false;
		this.hour = Integer.MIN_VALUE;
		this.minute = Integer.MIN_VALUE;
		this.timeZone = "";
		this.isRaining = false;
		this.isThundering = false;
		
		this.shouldTakeScreenShot = false;
		this.shouldIGoToNether = false;
	}
	
	// Send data through TCP socket (INetworkListener)
	public void sendEverything() {
		if (isWorldLoaded()) {
			nManager.sendPlayername(this.playerName);
			nManager.sendHealth(this.health);
			nManager.sendHunger(this.hunger);
			nManager.sendArmor(this.armor);
			nManager.sendExpLevel(this.expLvl);
			nManager.sendCoordX(this.x);
			nManager.sendCoordY(this.y);
			nManager.sendCoordZ(this.z);
			nManager.sendBiome(this.biome);
			nManager.sendCurrentItem(this.currentItem);
			
			nManager.sendWorldName(this.worldName);
			nManager.sendSeed(this.seed);
			nManager.sendDaytime(this.isDaytime);
			nManager.sendTime(this.hour, this.minute, this.timeZone);
			nManager.sendRaining(this.isRaining);
			nManager.sendThundering(this.isThundering);
		}
	}
	
	private void updatePlayername() {
		if (this.playerName != mc.thePlayer.getEntityName()) {
			this.playerName = mc.thePlayer.getEntityName();
			nManager.sendPlayername(mc.thePlayer.getEntityName());
		}
	}
	
	private void updateHealth() {
		int mHealth = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getEntityName()).getHealth();
		if (this.health != mHealth) {
			this.health = mHealth;
			nManager.sendHealth(mHealth);
		}
	}

	private void updateHunger() {
		int mHunger = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getEntityName()).getFoodStats().getFoodLevel();
		if (this.hunger != mHunger) {
			this.hunger = mHunger;
			nManager.sendHunger(mHunger);
		}
	}
	
	private void updateArmor() {
		int mArmor = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getEntityName()).getTotalArmorValue();
		if (this.armor != mArmor) {
			this.armor = mArmor;
			nManager.sendArmor(mArmor);
		}
	}
	
	private void updateExpLevel() {
		int mExpLvl = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getEntityName()).experienceLevel;
		if (this.expLvl != mExpLvl) {
			this.expLvl = mExpLvl;
			nManager.sendExpLevel(mExpLvl);
		}
	}
	
	private void updateCoords() {
		int mX = MathHelper.floor_double(mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getEntityName()).posX);
		int mY = MathHelper.floor_double(mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getEntityName()).posY);
		int mZ = MathHelper.floor_double(mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getEntityName()).posZ);
		
		if (this.x != mX) {
			this.x = mX;
			nManager.sendCoordX(mX);
		}
		
		if (this.y != mY) {
			this.y = mY;
			nManager.sendCoordY(mY);
		}
		
		if (this.z != mZ) {
			this.z = mZ;
			nManager.sendCoordZ(mZ);
		}
		
	}
	
	private void updateBiome() {
		String mBiome;
		int mX = MathHelper.floor_double(mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getEntityName()).posX);
		int mZ = MathHelper.floor_double(mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getEntityName()).posZ);
		Chunk mChunk = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getChunkFromBlockCoords(mX, mZ);
		mBiome = mChunk.getBiomeGenForWorldCoords(mX & 15, mZ & 15, mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldChunkManager()).biomeName;
		
		if (!this.biome.equals(mBiome)) {
			this.biome = mBiome;
			nManager.sendBiome(mBiome);
		}
	}
	
	private void updateCurrentItem() {
		if (mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getEntityName()).inventory.getCurrentItem() == null && !this.currentItem.equals("")) {
			this.currentItem = "";
			nManager.sendCurrentItem("null");
		} else if (mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getEntityName()).inventory.getCurrentItem() != null) {
			if (!mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getEntityName()).inventory.getCurrentItem().getDisplayName().equals(this.currentItem)) {
				this.currentItem = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getEntityName()).inventory.getCurrentItem().getDisplayName();
				nManager.sendCurrentItem(mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getEntityName()).inventory.getCurrentItem().getDisplayName());
			}
		}
	}
	
	private void updateWorldname() {
		String mWorldname;
		if (mc.isIntegratedServerRunning()) {
			mWorldname = mc.getIntegratedServer().getWorldName();
		} else {
			if (!mc.theWorld.getWorldInfo().getWorldName().isEmpty()) {
				mWorldname = mc.theWorld.getWorldInfo().getWorldName();
			} else {
				mWorldname = "";
			}
		}
		
		if (!this.worldName.equals(mWorldname)) {
			this.worldName = mWorldname;
			nManager.sendWorldName(mWorldname);
		}
	}
	
	private void updateSeed() {
		long mSeed = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldInfo().getSeed();
		if (this.seed != mSeed) {
			this.seed = mSeed;
			nManager.sendSeed(mSeed);
		}
	}
	
	private void updateDaytime() {
		long mTime = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldInfo().getWorldTime() - 24000 * (int) (mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldInfo().getWorldTime() / 24000);
		if (mTime >= 0 && mTime < 12000) {
			if (!this.isDaytime) {
				this.isDaytime = true;
				nManager.sendDaytime(true);
			}
		} else {
			if (this.isDaytime) {
				this.isDaytime = false;
				nManager.sendDaytime(false);
			}
		}
	}
	
	private void updateWeather() {
		if (!this.isRaining && mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldInfo().isRaining()) {
			this.isRaining = true;
			nManager.sendRaining(true);
		} else if (this.isRaining && !mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldInfo().isRaining()) {
			this.isRaining = false;
			nManager.sendRaining(false);
		}
		
		if (!this.isThundering && mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldInfo().isThundering()) {
			this.isThundering = true;
			nManager.sendThundering(true);
		} else if (this.isThundering && !mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldInfo().isThundering()) {
			this.isThundering = false;
			nManager.sendThundering(false);
		}
	}
	
	private void updateTime() {
		long mTime;
		if (mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldInfo().getWorldTime() > 24000) {
			mTime = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldInfo().getWorldTime() - 24000 * (int) (mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldInfo().getWorldTime() / 24000);
		} else {
			mTime = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldInfo().getWorldTime();
		}
		
		int mHour, mMinute;
		String mTimeZone = "";
		
		// Calculate minutes
		if ( ((mTime * 60) / 1000) > 60 ) {
			mMinute = (int) ((mTime * 60) / 1000) - (60 * ((int) mTime / 1000));
		} else {
			mMinute = (int) ((mTime * 60) / 1000);
		}
		
		// if minutes == 0 or 15 or 30 or 45, then update and send
		if (mMinute == 0 || mMinute == 15 || mMinute == 30 || mMinute == 45) {
			
			// Calculate hour
			if ( (((int) mTime / 1000) + 6) > 23 ) {
				mHour = (((int) mTime / 1000) + 6) - 24;
			} else {
				mHour = ((int) mTime / 1000) + 6;
			}
			
			// Set hour from 24 to 12
			if (mHour > 11) {
				mHour = mHour - 12;
				mTimeZone = "pm";
			} else {
				mTimeZone = "am";
			}
			
			// Set 0 to 12
			if (mHour == 0) {
				mHour = 12;
			}
			
			if (this.hour != mHour || this.minute != mMinute || !this.timeZone.equals(mTimeZone)) {
				this.hour = mHour;
				this.minute = mMinute;
				this.timeZone = mTimeZone;
				nManager.sendTime(this.hour, this.minute, this.timeZone);
			}
		}
	}
	
	// NetworkManager Interface
	
	public void setHealth(int mHealth) {
		mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getEntityName()).setEntityHealth(mHealth);
	}
	
	public void setHunger(int mHunger) {
		mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getEntityName()).getFoodStats().setFoodLevel(mHunger);
	}
	
	public void setExpLvl(int mExpLvl) {
		mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getEntityName()).addExperienceLevel(mExpLvl);
	}
	
	public void toggleGameMode() {
		if (mc.playerController.isInCreativeMode()) {
			mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getEntityName()).setGameType(EnumGameType.SURVIVAL);
		} else {
			mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getEntityName()).setGameType(EnumGameType.CREATIVE);
		}
	}
	
	public void setWorldTime(String dayOrNight) {
		if (dayOrNight.equalsIgnoreCase("Day")) {
			mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).setWorldTime(0);
		} else if (dayOrNight.equalsIgnoreCase("Night")) {
			mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).setWorldTime(12500);
		}
	}
	
	public void setWorldWeather() {
		mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).toggleRain();
	}
	
	public void enableScreenShot() {
		shouldTakeScreenShot = true;
	}
	
	// Apparently, it's not possible to call ScreenShotHelper.saveScreenshot() from the NetworkManager thread, so I'm using a flag (shouldTakeScreenShot)
	public void getScreenshot() {
		if (shouldTakeScreenShot) {
			shouldTakeScreenShot = false;
			try {
				String fileName = ScreenShotHelper.saveScreenshot(mc.getMinecraftDir().getCanonicalFile(), mc.displayWidth, mc.displayHeight);
				fileName = fileName.split(" ")[3];
				nManager.sendScreenShot(fileName, seed);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void teleportTo(int mDim, int x, int y, int z) {
		if (mc.thePlayer.dimension == mDim) {
			mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getEntityName()).setPositionAndUpdate(x, y, z);			
		} else {
			EntityPlayerMP mPlayer = (EntityPlayerMP) mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getEntityName());
			mPlayer.mcServer.getConfigurationManager().transferPlayerToDimension(mPlayer, mDim, new MyTeleporter( mc.getIntegratedServer().worldServerForDimension(mDim), x, y, z ));			
		}
	}
	
	public void toggleButton(int mDim, int x, int y, int z) {
		final int mButtonStone = 77;
		final int mButtonWood = 143;
		
		WorldServer mWorld = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension);
		
		if (mDim == mc.thePlayer.dimension) {
			if (mWorld.blockExists(x, y, z)) {
				if (mWorld.getBlockId(x, y, z) == mButtonStone || mWorld.getBlockId(x, y, z) == mButtonWood) {
					int blockID = mWorld.getBlockId(x, y, z);
					// Time period the button will remain active (20 = stone, 30 = wooden)
					int tickRate = (blockID == 77) ? 20 : 30;
					
					// Activate button
		            int i1 = mWorld.getBlockMetadata(x, y, z);
		            int j1 = i1 & 7;
		            int k1 = 8 - (i1 & 8);
		            if (k1 != 0) {
		            	mWorld.setBlockMetadataWithNotify(x, y, z, j1+k1, 3);
		            	mWorld.markBlockRangeForRenderUpdate(x, y, z, x, y, z);
		            	mWorld.playSoundEffect((double) x + 0.5D, (double) y + 0.5D, (double) z + 0.5D, "random.click", 0.3F, 0.6F);
		            	mWorld.scheduleBlockUpdate(x, y, z, blockID, tickRate);
		            	
		            	// Notify block update
		            	mWorld.notifyBlocksOfNeighborChange(x, y, z, blockID);
	
		                if (j1 == 1) {
		                	mWorld.notifyBlocksOfNeighborChange(x - 1, y, z, blockID);
		                } else if (j1 == 2) {
		                	mWorld.notifyBlocksOfNeighborChange(x + 1, y, z, blockID);
		                } else if (j1 == 3) {
		                	mWorld.notifyBlocksOfNeighborChange(x, y, z - 1, blockID);
		                } else if (j1 == 4) {
		                	mWorld.notifyBlocksOfNeighborChange(x, y, z + 1, blockID);
		                } else {
		                	mWorld.notifyBlocksOfNeighborChange(x, y - 1, z, blockID);
		                }
		            }
					
				} else {
					mc.ingameGUI.getChatGUI().printChatMessage("[Remotecraft] Error. Block is not a Button.");
				}
			} else {
				mc.ingameGUI.getChatGUI().printChatMessage("[Remotecraft] Error. No block found.");
			}
		} else {
			mc.ingameGUI.getChatGUI().printChatMessage("[Remotecraft] Error. Not in the same dimension.");
		}
	}
	
	public void toggleLever(int mDim, int x, int y, int z) {
		final int mLever = 69;
		
		WorldServer mWorld = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension);
		
		if (mDim == mc.thePlayer.dimension) {
			if (mWorld.blockExists(x, y, z)) {
				if (mWorld.getBlockId(x, y, z) == mLever) {
					
					// Activate lever
		            int i1 = mWorld.getBlockMetadata(x, y, z);
		            int j1 = i1 & 7;
		            int k1 = 8 - (i1 & 8);
		            
		            mWorld.setBlockMetadataWithNotify(x, y, z, j1+k1, 3);
		            mWorld.playSoundEffect((double) x + 0.5D, (double) y + 0.5D, (double) z + 0.5D, "random.click", 0.3F, k1 > 0 ? 0.6F : 0.5F);
		            mWorld.notifyBlocksOfNeighborChange(x, y, z, 69);
	
		            if (j1 == 1) {
		            	mWorld.notifyBlocksOfNeighborChange(x - 1, y, z, 69);
		            } else if (j1 == 2) {
		            	mWorld.notifyBlocksOfNeighborChange(x + 1, y, z, 69);
		            } else if (j1 == 3) {
		            	mWorld.notifyBlocksOfNeighborChange(x, y, z - 1, 69);
		            } else if (j1 == 4) {
		            	mWorld.notifyBlocksOfNeighborChange(x, y, z + 1, 69);
		            } else if (j1 != 5 && j1 != 6) {
		                if (j1 == 0 || j1 == 7) {
		                	mWorld.notifyBlocksOfNeighborChange(x, y + 1, z, 69);
		                }
		            } else {
		            	mWorld.notifyBlocksOfNeighborChange(x, y - 1, z, 69);
		            }
		            
				} else {
					mc.ingameGUI.getChatGUI().printChatMessage("[Remotecraft] Error. Block is not a Lever.");
				}
			} else {
				mc.ingameGUI.getChatGUI().printChatMessage("[Remotecraft] Error. No block found.");
			}
		} else {
			mc.ingameGUI.getChatGUI().printChatMessage("[Remotecraft] Error. Not in the same dimension.");
		}
	}
	
	// This method is called everytime the WorldFragment is created in the Android app
	public void forceSendWorldInfo() {
		forceUpdateBiome();
		forceUpdateDaytime();
		forceUpdateWeather();
		forceUpdateTime();
	}
	
	private void forceUpdateBiome() {
		String mBiome;
		int mX = MathHelper.floor_double(mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getEntityName()).posX);
		int mZ = MathHelper.floor_double(mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getEntityName()).posZ);
		Chunk mChunk = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getChunkFromBlockCoords(mX, mZ);
		mBiome = mChunk.getBiomeGenForWorldCoords(mX & 15, mZ & 15, mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldChunkManager()).biomeName;
		
		this.biome = mBiome;
		nManager.sendBiome(mBiome);
	}
	
	private void forceUpdateDaytime() {
		long mTime = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldInfo().getWorldTime() - 24000 * (int) (mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldInfo().getWorldTime() / 24000);
		if (mTime >= 0 && mTime < 12000) {
			this.isDaytime = true;
			nManager.sendDaytime(true);
		} else {
			this.isDaytime = false;
			nManager.sendDaytime(false);
		}
	}
	
	private void forceUpdateWeather() {
		if (mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldInfo().isRaining()) {
			this.isRaining = true;
			nManager.sendRaining(true);
		} else {
			this.isRaining = false;
			nManager.sendRaining(false);
		}
		
		if (mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldInfo().isThundering()) {
			this.isThundering = true;
			nManager.sendThundering(true);
		} else {
			this.isThundering = false;
			nManager.sendThundering(false);
		}
	}
	
	private void forceUpdateTime() {
		long mTime;
		if (mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldInfo().getWorldTime() > 24000) {
			mTime = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldInfo().getWorldTime() - 24000 * (int) (mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldInfo().getWorldTime() / 24000);
		} else {
			mTime = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldInfo().getWorldTime();
		}
		
		int mHour, mMinute;
		String mTimeZone = "";
		
		// Calculate minutes
		if ( ((mTime * 60) / 1000) > 60 ) {
			mMinute = (int) ((mTime * 60) / 1000) - (60 * ((int) mTime / 1000));
		} else {
			mMinute = (int) ((mTime * 60) / 1000);
		}
		
		// Calculate hour
		if ( (((int) mTime / 1000) + 6) > 23 ) {
			mHour = (((int) mTime / 1000) + 6) - 24;
		} else {
			mHour = ((int) mTime / 1000) + 6;
		}
		
		// Set hour from 24 to 12
		if (mHour > 11) {
			mHour = mHour - 12;
			mTimeZone = "pm";
		} else {
			mTimeZone = "am";
		}
		
		// Set 0 to 12
		if (mHour == 0) {
			mHour = 12;
		}
		
		this.hour = mHour;
		this.minute = mMinute;
		this.timeZone = mTimeZone;
		nManager.sendTime(this.hour, this.minute, this.timeZone);
		
	}
	
	private class MyTeleporter extends Teleporter {
        private Random random;
        int x, y, z;
        
        public MyTeleporter(WorldServer par1WorldServer, int x, int y, int z) {
	        super(par1WorldServer);
	        random = new Random();
	        this.x = x;
	        this.y = y;
	        this.z = z;
        }
        
        @Override
        public void placeInPortal(Entity par1Entity, double par2, double par4, double par6, float par8) {
        	par1Entity.setLocationAndAngles(x, y, z, par1Entity.rotationYaw, par1Entity.rotationPitch);
        }
        
        @Override
        public boolean placeInExistingPortal(Entity par1Entity, double par2, double par4, double par6, float par8) {
	        return false;
        }

        @Override
        public boolean makePortal(Entity ent) {
	        return true;
        }

        @Override
        public void removeStalePortalLocations(long l) {
	        //
        }
	}
}
