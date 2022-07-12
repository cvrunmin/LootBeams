package com.lootbeams.mixin;

import com.lootbeams.LootBeamRenderer;
import com.lootbeams.LootBeams;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.List;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void attemptRenderBeams(Entity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci){
        if (entity instanceof ItemEntity) {
            ItemEntity itemEntity = (ItemEntity) entity;
            if (Minecraft.getInstance().player.distanceToSqr(itemEntity) > LootBeams.config.renderDistance * LootBeams.config.renderDistance) {
                return;
            }

            boolean shouldRender = false;
            if (LootBeams.config.allItems) {
                shouldRender = true;
            } else {
                if (LootBeams.config.onlyEquipment) {
                    List<Class<? extends Item>> equipmentClasses = Arrays.asList(SwordItem.class, DiggerItem.class, ArmorItem.class, ShieldItem.class, BowItem.class, CrossbowItem.class, TridentItem.class, ArrowItem.class, FishingRodItem.class);
                    for (Class<? extends Item> item : equipmentClasses) {
                        if (item.isAssignableFrom(itemEntity.getItem().getItem().getClass())) {
                            shouldRender = true;
                            break;
                        }
                    }
                }

                if (LootBeams.config.onlyRare) {
                    shouldRender = itemEntity.getItem().getRarity() != Rarity.COMMON;
                }

                if (isItemInRegistryList(LootBeams.config.whitelist, itemEntity.getItem().getItem())) {
                    shouldRender = true;
                }
            }
            if (isItemInRegistryList(LootBeams.config.blacklist, itemEntity.getItem().getItem())) {
                shouldRender = false;
            }

            if (shouldRender) {
                LootBeamRenderer.renderLootBeam(poseStack, buffer, partialTick, itemEntity.level.getGameTime(), itemEntity);
            }
        }
    }

    /**
     * Checks if the given item is in the given list of registry names.
     */
    private static boolean isItemInRegistryList(List<String> registryNames, Item item) {
        if (registryNames.size() > 0) {
            for (String id : registryNames.stream().filter((s) -> (!s.isEmpty())).toList()) {
                if (!id.contains(":")) {
                    if (Registry.ITEM.getKey(item).getNamespace().equals(id)) {
                        return true;
                    }
                }
                ResourceLocation itemResource = ResourceLocation.tryParse(id);
                if (itemResource != null && Registry.ITEM.get(itemResource).asItem() == item.asItem()) {
                    return true;
                }
            }
        }
        return false;
    }
}
