package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ExampleMod implements ModInitializer {
    public static final String MOD_ID = "modid";

    public static final Item ZOILT_BLADE = new ZoiltBladeItem(
            ToolMaterials.NETHERITE, 4, -2.0f,
            new FabricItemSettings().rarity(Rarity.EPIC).fireproof()
    );

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "zoilt_blade"), ZOILT_BLADE);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(content -> {
            content.add(ZOILT_BLADE);
        });
    }

    public static class ZoiltBladeItem extends SwordItem {
        public ZoiltBladeItem(ToolMaterial material, int attackDamage, float attackSpeed, Settings settings) {
            super(material, attackDamage, attackSpeed, settings);
        }

        // Даём клинку красивое магическое свечение (как у зачарованного предмета)
        @Override
        public boolean hasGlint(ItemStack stack) {
            return true;
        }

        // 1. Правый клик — Изумрудный рывок вперёд
        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            ItemStack stack = user.getStackInHand(hand);
            Vec3d look = user.getRotationVector();

            double dashPower = 1.65;
            user.setVelocity(look.x * dashPower, look.y * 0.9 + 0.35, look.z * dashPower);
            user.velocityModified = true;

            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 0.7f, 1.6f);
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0f, 1.2f);

            if (!world.isClient && world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                        user.getX(), user.getY() + 1.0, user.getZ(),
                        25, 0.4, 0.5, 0.4, 0.15);
                serverWorld.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING,
                        user.getX(), user.getY() + 1.0, user.getZ(),
                        15, 0.3, 0.3, 0.3, 0.2);

                user.fallDistance = 0.0f;
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 50, 0, false, false, true));
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 60, 1, false, false, true));
                user.getItemCooldownManager().set(this, 40);
            }

            return TypedActionResult.success(stack, world.isClient());
        }

        // 2. Удар в прыжке — Магический разряд
        @Override
        public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            World world = attacker.getWorld();
            if (!attacker.isOnGround() && !world.isClient && world instanceof ServerWorld serverWorld) {
                target.damage(world.getDamageSources().magic(), 6.0f);
                target.addVelocity(0, 0.45, 0);
                target.velocityModified = true;

                serverWorld.spawnParticles(ParticleTypes.SCRAPE,
                        target.getX(), target.getY() + 1.0, target.getZ(),
                        30, 0.4, 0.6, 0.4, 0.2);
                world.playSound(null, target.getBlockPos(),
                        SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0f, 0.8f);
            }
            return super.postHit(stack, target, attacker);
        }

        // 3. Описание и название
        @Override
        public Text getName(ItemStack stack) {
            return Text.literal("Эльфийский клинок Зоилта").formatted(Formatting.DARK_GREEN, Formatting.BOLD);
        }

        @Override
        public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
            tooltip.add(Text.literal("Древний клинок с золотой гравировкой").formatted(Formatting.GREEN, Formatting.ITALIC));
            tooltip.add(Text.literal("• [ПКМ] Изумрудный рывок ветра").formatted(Formatting.AQUA));
            tooltip.add(Text.literal("• [В прыжке] Магический срез (+6 урона)").formatted(Formatting.GOLD));
        }
    }
}
