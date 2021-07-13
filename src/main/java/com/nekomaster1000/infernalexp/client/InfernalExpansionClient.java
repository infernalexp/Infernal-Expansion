package com.nekomaster1000.infernalexp.client;

import com.nekomaster1000.infernalexp.InfernalExpansion;
import com.nekomaster1000.infernalexp.config.gui.screens.ConfigScreen;
import com.nekomaster1000.infernalexp.init.IEItems;
import com.nekomaster1000.infernalexp.items.WhipItem;
import net.minecraft.item.ItemModelsProperties;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

@OnlyIn(Dist.CLIENT)
public class InfernalExpansionClient {
    public static void init() {
        // Register GUI Factories
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY, () -> (mc, screen) -> new ConfigScreen());

        MinecraftForge.EVENT_BUS.addListener((LivingUpdateEvent event) -> DynamicLightingHandler.tick(event.getEntityLiving()));

        ItemModelsProperties.registerProperty(IEItems.GLOWSILK_BOW.get(), new ResourceLocation("pull"), (itemStack, clientWorld, livingEntity) -> {
          if (livingEntity == null) {
              return 0.0F;
          } else {
              return livingEntity.getActiveItemStack() != itemStack ? 0.0F : (float) (itemStack.getUseDuration() - livingEntity.getItemInUseCount()) / 20.0F;
        }});
        
        ItemModelsProperties.registerProperty(IEItems.GLOWSILK_BOW.get(), new ResourceLocation("pulling"), (itemStack, clientWorld, livingEntity) -> livingEntity != null && livingEntity.isHandActive() && livingEntity.getActiveItemStack() == itemStack ? 1.0F : 0.0F);

        ItemModelsProperties.registerProperty(IEItems.BLINDSIGHT_TONGUE_WHIP.get(), new ResourceLocation("attack_frame"), (itemStack, clientWorld, livingEntity) ->
            livingEntity == null || livingEntity.getHeldItemMainhand() != itemStack ?
                0 : (int) (((WhipItem) itemStack.getItem()).getTicksSinceAttack(itemStack) / 6.0F)
        );

        ItemModelsProperties.registerProperty(IEItems.BLINDSIGHT_TONGUE_WHIP.get(), new ResourceLocation("attacking"), (itemStack, clientWorld, livingEntity) -> livingEntity != null && (((WhipItem) itemStack.getItem()).getAttacking(itemStack) || ((WhipItem) itemStack.getItem()).getCharging(itemStack)) && livingEntity.getHeldItemMainhand() == itemStack ? 1.0F : 0.0F);

        InfernalExpansionClient.loadInfernalResources();
    }

    public static void loadInfernalResources() {
        // Creates file location for resource pack
        File dir = new File(".", "resourcepacks");
        File target = new File(dir, "Infernal Resources.zip");

        // If the pack isn't already in the folder, copies the file over from the mod files
        if (!target.exists()) {
            try {
                dir.mkdirs();
                InputStream in = InfernalExpansion.class.getResourceAsStream("/assets/infernalexp/infernal_resources.zip");
                FileOutputStream out = new FileOutputStream(target);

                // The number of bytes here is how many can be read from the resource pack at one time
                // 16kB is the most common disk chunk size, and using this array size
                // reduces latency between reading and actually processing the data.
                // The performance difference is not significant, but it's improved by using a 16kB array.
                byte[] buf = new byte[16384];
                int len = 0;
                while((len = in.read(buf)) > 0)
                    out.write(buf, 0, len);

                in.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
