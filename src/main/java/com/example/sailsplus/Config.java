package com.example.sailsplus;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

// Configuration class for Sails+ mod
// This is a clean config setup ready for future configuration options
@Mod.EventBusSubscriber(modid = SailsPlusMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // Future configuration options will be added here as needed
    // For now, this is just the basic structure

    static final ForgeConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        // Configuration loading will be handled here when we add config options
    }
}
