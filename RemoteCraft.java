package com.zireck.remotecraft;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Init;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;

@Mod(modid="Remotecraft", name="Remotecraft", version="1.0")
@NetworkMod(clientSideRequired=true)
public class RemoteCraft {

        // The instance of your mod that Forge uses.
        @Instance(value = "Remotecraft")
        public static RemoteCraft instance;
        
        // Says where the client and server 'proxy' code is loaded.
        @SidedProxy(clientSide="com.zireck.remotecraft.client.ClientProxy", serverSide="com.zireck.remotecraft.CommonProxy")
        public static CommonProxy proxy;
        
        @PreInit
        public void preInit(FMLPreInitializationEvent event) {
	        // Stub Method
        }
        
        @Init
        public void load(FMLInitializationEvent event) {
	        proxy.registerRenderers();
	        TickRegistry.registerTickHandler(new Core(), Side.CLIENT);
        }
        
        @PostInit
        public void postInit(FMLPostInitializationEvent event) {
            // Stub Method
        }
}