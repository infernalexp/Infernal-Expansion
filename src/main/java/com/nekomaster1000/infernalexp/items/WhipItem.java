package com.nekomaster1000.infernalexp.items;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.enchantment.IVanishable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.IItemTier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TieredItem;
import net.minecraft.item.UseAction;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import net.minecraftforge.common.ForgeMod;

public class WhipItem extends TieredItem implements IWhipItem, IVanishable {

	private final float attackDamage;
	private final float attackSpeed;
	private final float attackKnockback;

	private boolean shouldKnockback = false;

	private int ticksSinceAttack = 0;
	private boolean attacking = false;
	private boolean charging = false;

	public WhipItem(IItemTier tier, float attackDamageIn, float attackSpeedIn, float attackKnockbackIn, Item.Properties builderIn) {
		super(tier, builderIn);
		this.attackDamage = attackDamageIn + tier.getAttackDamage();
		this.attackSpeed = attackSpeedIn;
		this.attackKnockback = attackKnockbackIn;
	}

	public void onPlayerStoppedUsing(ItemStack stack, World worldIn, LivingEntity entityLiving, int timeLeft) {
		if (entityLiving instanceof PlayerEntity) {
			PlayerEntity playerentity = (PlayerEntity)entityLiving;

			this.charging = false;

			int i = this.getUseDuration(stack) - timeLeft;

			if (i < 0 || timeLeft > 71985) {
				this.ticksSinceAttack = 0;
				return;
			} else {
				this.setAttacking(true);
                this.ticksSinceAttack = 36;
			}

			worldIn.playSound(null, playerentity.getPosX(), playerentity.getPosY(), playerentity.getPosZ(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1.0F, 1.0F / (random.nextFloat() * 0.4F + 1.2F));

			double reach = playerentity.getAttribute(ForgeMod.REACH_DISTANCE.get()).getValue();

			Vector3d eyePos = playerentity.getEyePosition(1.0F);
			Vector3d lookVec = playerentity.getLookVec();
			Vector3d reachVec = eyePos.add(lookVec.mul(reach, reach, reach));

			AxisAlignedBB playerBox = playerentity.getBoundingBox().expand(lookVec.scale(reach)).grow(1.0D, 1.0D, 1.0D);
			EntityRayTraceResult traceResult = ProjectileHelper.rayTraceEntities(playerentity, eyePos, reachVec, playerBox, (target) -> !target.isSpectator() && target.isLiving(), reach * reach);

			if (traceResult != null) {
				this.shouldKnockback = true;
				playerentity.attackTargetEntityWithCurrentItem(traceResult.getEntity());
				playerentity.ticksSinceLastSwing = (int) playerentity.getCooldownPeriod();
			}

			playerentity.addStat(Stats.ITEM_USED.get(this));
		}
	}

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

	public int getUseDuration(ItemStack stack) {
		return 72000;
	}

	public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
		ItemStack itemstack = playerIn.getHeldItem(handIn);
		playerIn.setActiveHand(handIn);
		return ActionResult.resultPass(itemstack);
	}

	@Override
	public void onUsingTick(ItemStack stack, LivingEntity player, int count) {
		if (this.attacking) {
			this.ticksSinceAttack = 0;
			this.attacking = false;
		}
		this.charging = true;
	}

	@Override
	public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
		if (this.charging && this.ticksSinceAttack <= 30) {
			this.ticksSinceAttack++;
		}

		if (this.attacking) {
			this.ticksSinceAttack++;
		}

		if (this.ticksSinceAttack >= 60) {
			this.ticksSinceAttack = 0;
			this.attacking = false;
			this.charging = false;
		}
	}

	public boolean canPlayerBreakBlockWhileHolding(BlockState state, World worldIn, BlockPos pos, PlayerEntity player) {
		return !player.isCreative();
	}

	public boolean hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		super.hitEntity(stack, target, attacker);

		if (this.shouldKnockback) {
			target.applyKnockback(this.attackKnockback, MathHelper.sin(attacker.rotationYaw * ((float) Math.PI / 180F)), -MathHelper.cos(attacker.rotationYaw * ((float) Math.PI / 180F)));
			this.shouldKnockback = false;
		}

		stack.damageItem(1, attacker, (entity) -> {
			entity.sendBreakAnimation(EquipmentSlotType.MAINHAND);
		});
		return true;
	}

	public boolean onBlockDestroyed(ItemStack stack, World worldIn, BlockState state, BlockPos pos, LivingEntity entityLiving) {
		if (state.getBlockHardness(worldIn, pos) != 0.0F) {
			stack.damageItem(2, entityLiving, (entity) -> {
				entity.sendBreakAnimation(EquipmentSlotType.MAINHAND);
			});
		}

		return true;
	}

	public float getDestroySpeed(ItemStack stack, BlockState state) {
		Material material = state.getMaterial();
		return material != Material.PLANTS && material != Material.TALL_PLANTS && material != Material.CORAL && !state.isIn(BlockTags.LEAVES) && material != Material.GOURD ? 1.0F : 1.5F;
	}

	public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
		return enchantment.type == (EnchantmentType.WEAPON);
	}

	public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlotType equipmentSlot, ItemStack itemStack) {
		ImmutableMultimap.Builder<Attribute, AttributeModifier> attributeBuilder = ImmutableMultimap.builder();
		attributeBuilder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", this.attackDamage, AttributeModifier.Operation.ADDITION));
		attributeBuilder.put(Attributes.ATTACK_SPEED, new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", this.attackSpeed, AttributeModifier.Operation.ADDITION));
		attributeBuilder.put(ForgeMod.REACH_DISTANCE.get(), new AttributeModifier(REACH_MODIFIER_UUID, "Tool modifier", this.getReachDistanceModifier(), AttributeModifier.Operation.ADDITION));
		Multimap<Attribute, AttributeModifier> attributes = attributeBuilder.build();
		return equipmentSlot == EquipmentSlotType.MAINHAND ? attributes : super.getAttributeModifiers(equipmentSlot, itemStack);
	}

	@Override
	public int getTicksSinceAttack() {
		return this.ticksSinceAttack;
	}

	@Override
	public boolean getAttacking() {
		return this.attacking;
	}

	@Override
	public boolean getCharging() {
		return this.charging;
	}

	@Override
	public void setAttacking(boolean value) {
		this.attacking = value;
	}
}
