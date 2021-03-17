package com.nekomaster1000.infernalexp;

import com.nekomaster1000.infernalexp.brewing.IEBrewingRecipe;
import com.nekomaster1000.infernalexp.client.InfernalExpansionClient;
import com.nekomaster1000.infernalexp.config.ConfigHelper;
import com.nekomaster1000.infernalexp.config.ConfigHolder;
import com.nekomaster1000.infernalexp.config.gui.screens.ConfigScreen;
import com.nekomaster1000.infernalexp.events.MiscEvents;
import com.nekomaster1000.infernalexp.events.MobEvents;
import com.nekomaster1000.infernalexp.events.WorldEvents;
import com.nekomaster1000.infernalexp.init.*;
import com.nekomaster1000.infernalexp.world.dimension.ModNetherBiomeCollector;
import com.nekomaster1000.infernalexp.world.dimension.ModNetherBiomeProvider;
import com.nekomaster1000.infernalexp.world.gen.ModEntityPlacement;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowerPotBlock;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtils;
import net.minecraft.potion.Potions;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("infernalexp")
public class InfernalExpansion
{
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MOD_ID = "infernalexp";

    public InfernalExpansion()
    {
        final ModLoadingContext modLoadingContext = ModLoadingContext.get();
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::commonSetup);

        // Register GUI Factories
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY, () -> (mc, screen) -> new ConfigScreen());

        //Registering deferred registers to the mod bus
        IEParticleTypes.PARTICLES.register(modEventBus);
        IEBlocks.register(modEventBus);
        IEItems.register(modEventBus);
        IEEffects.register(modEventBus);
        IEPotions.register(modEventBus);
        IEEntityTypes.register(modEventBus);
        IEPaintings.register(modEventBus);
        IETileEntityTypes.register(modEventBus);
        IEBiomes.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new MiscEvents());
        MinecraftForge.EVENT_BUS.register(new MobEvents());
        MinecraftForge.EVENT_BUS.register(new WorldEvents());


        //Registering Configs
        modLoadingContext.registerConfig(ModConfig.Type.CLIENT, ConfigHolder.CLIENT_SPEC);
        modLoadingContext.registerConfig(ModConfig.Type.COMMON, ConfigHolder.COMMON_SPEC);

        //Baking Configs
        ConfigHelper.bakeClient(null);
        ConfigHelper.bakeCommon(null);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        //Search for all biomes to add to nether and register nether biome provider
        ModNetherBiomeCollector.netherBiomeCollection();
        Registry.register(Registry.BIOME_PROVIDER_CODEC, new ResourceLocation(MOD_ID, "infernalexp_nether"), ModNetherBiomeProvider.MOD_NETHER_CODEC);

        //Setup and register structures
        event.enqueueWork(IEStructures::setupStructures);

        //Places entity spawn locations on the ground
        ModEntityPlacement.spawnPlacement();

        //Register New Flowers to be Able to Place in Pots
        FlowerPotBlock flowerPot = (FlowerPotBlock) Blocks.FLOWER_POT;
        flowerPot.addPlant(IEBlocks.DULLTHORNS.getId(), IEBlocks.POTTED_DULLTHORNS);
        flowerPot.addPlant(IEBlocks.LUMINOUS_FUNGUS.getId(), IEBlocks.POTTED_LUMINOUS_FUNGUS);
        flowerPot.addPlant(IEBlocks.SHROOMLIGHT_FUNGUS.getId(), IEBlocks.POTTED_SHROOMLIGHT_FUNGUS);

        //Register Brewing Recipes for Potions
        BrewingRecipeRegistry.addRecipe(new IEBrewingRecipe(
                PotionUtils.addPotionToItemStack(Items.POTION.getDefaultInstance(), Potions.AWKWARD),
                IEItems.MOTH_DUST.get().getDefaultInstance(),
                PotionUtils.addPotionToItemStack(Items.POTION.getDefaultInstance(), IEPotions.LUMINOUS.get())));
        BrewingRecipeRegistry.addRecipe(new IEBrewingRecipe(
                PotionUtils.addPotionToItemStack(Items.POTION.getDefaultInstance(), IEPotions.LUMINOUS.get()),
                Items.GUNPOWDER.getDefaultInstance(),
                PotionUtils.addPotionToItemStack(Items.SPLASH_POTION.getDefaultInstance(), IEPotions.LUMINOUS.get())));
        BrewingRecipeRegistry.addRecipe(new IEBrewingRecipe(
                PotionUtils.addPotionToItemStack(Items.SPLASH_POTION.getDefaultInstance(), IEPotions.LUMINOUS.get()),
                Items.DRAGON_BREATH.getDefaultInstance(),
                PotionUtils.addPotionToItemStack(Items.LINGERING_POTION.getDefaultInstance(), IEPotions.LUMINOUS.get())));
        BrewingRecipeRegistry.addRecipe(new IEBrewingRecipe(
                PotionUtils.addPotionToItemStack(Items.POTION.getDefaultInstance(), IEPotions.LUMINOUS.get()),
                Items.REDSTONE.getDefaultInstance(),
                PotionUtils.addPotionToItemStack(Items.POTION.getDefaultInstance(), IEPotions.LONG_LUMINOUS.get())));
        BrewingRecipeRegistry.addRecipe(new IEBrewingRecipe(
                PotionUtils.addPotionToItemStack(Items.POTION.getDefaultInstance(), IEPotions.LUMINOUS.get()),
                Items.GLOWSTONE_DUST.getDefaultInstance(),
                PotionUtils.addPotionToItemStack(Items.POTION.getDefaultInstance(), IEPotions.STRONG_LUMINOUS.get())));
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> InfernalExpansionClient::init);
    }

    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        IECommands.registerCommands(event.getServer().getCommandManager().getDispatcher());
    }

    public static final ItemGroup TAB = new ItemGroup("InfernalTab") {

        @Override
        public ItemStack createIcon() {
            return new ItemStack(IEItems.BLINDSIGHT_TONGUE.get());
        }

    };

}