package com.tpmod;

import com.tpmod.command.TPCommands;
import com.tpmod.data.TPDataManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("glidetp")
public class TPMod {
    
    public TPMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    private void setup(final FMLCommonSetupEvent event) {
        // 초기화 로직
    }
    
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        TPDataManager.loadData(event.getServer());
    }
    
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        TPCommands.register(event.getDispatcher());
    }
}
