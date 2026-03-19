package com.derko.advancedfoodsystem;

import com.derko.seamlessapi.SatiationAPI;
import com.derko.advancedfoodsystem.client.BuffHudRenderer;
import com.derko.advancedfoodsystem.client.ClientBuffState;
import com.derko.advancedfoodsystem.config.AfsClientConfig;
import com.derko.advancedfoodsystem.config.AfsConfig;
import com.derko.advancedfoodsystem.config.ConfigManager;
import com.derko.advancedfoodsystem.events.ClientEvents;
import com.derko.advancedfoodsystem.events.CommonEvents;
import com.derko.advancedfoodsystem.network.NetworkHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(AdvancedFoodSystemMod.MOD_ID)
public class AdvancedFoodSystemMod {
    public static final String MOD_ID = "advancedfoodsystem";

    public AdvancedFoodSystemMod(IEventBus modEventBus, ModContainer modContainer) {
        // Register NeoForge configs — both appear in the in-game Mods > Config screen
        modContainer.registerConfig(ModConfig.Type.COMMON, AfsConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, AfsClientConfig.SPEC);

        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onLoadComplete);
        modEventBus.addListener(this::onConfigLoad);
        modEventBus.addListener(this::onConfigReload);
        NetworkHandler.register(modEventBus);

        NeoForge.EVENT_BUS.register(CommonEvents.class);

        ConfigManager.loadOrCreate();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.register(BuffHudRenderer.class);
            NeoForge.EVENT_BUS.register(ClientBuffState.class);
            NeoForge.EVENT_BUS.register(ClientEvents.class);
        }
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ConfigManager::loadOrCreate);
    }

    /** Fires after all mod setup is complete — merge any API-registered foods. */
    private void onLoadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> ConfigManager.mergeApiRegistrations(SatiationAPI.freezeAndGetAll()));
    }

    private void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == AfsConfig.SPEC
                || event.getConfig().getSpec() == AfsClientConfig.SPEC) {
            ConfigManager.refreshFromNeoForgeConfigs();
        }
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == AfsConfig.SPEC
                || event.getConfig().getSpec() == AfsClientConfig.SPEC) {
            ConfigManager.refreshFromNeoForgeConfigs();
        }
    }
}
