package com.nekomaster1000.infernalexp.init;

import com.nekomaster1000.infernalexp.InfernalExpansion;
import com.nekomaster1000.infernalexp.world.biome.netherbiomes.GlowstoneCanyonBiome;
import com.nekomaster1000.infernalexp.world.dimension.ModNetherBiomeCatch;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class ModBiomes
{
    public static final DeferredRegister<Biome> BIOMES = DeferredRegister.create(ForgeRegistries.BIOMES, InfernalExpansion.MOD_ID);

    public static final RegistryObject<Biome> GLOWSTONE_CANYON = registerNetherBiome("glowstone_canyon", () -> new GlowstoneCanyonBiome().getBiome());

    private static RegistryObject<Biome> registerNetherBiome(String name, Supplier<Biome> biome) {
        ModNetherBiomeCatch.netherBiomeList.add(InfernalExpansion.MOD_ID + ":" + name);
        return BIOMES.register(name, biome);
    }

    public static void register(IEventBus eventBus) {
        BIOMES.register(eventBus);
        InfernalExpansion.LOGGER.info("Infernal Expansion: Biomes Registered!");
    }
}

