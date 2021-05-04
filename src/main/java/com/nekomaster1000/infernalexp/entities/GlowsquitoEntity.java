package com.nekomaster1000.infernalexp.entities;

import java.util.EnumSet;
import java.util.Random;

import javax.annotation.Nullable;

import com.nekomaster1000.infernalexp.entities.ai.TargetWithEffectGoal;
import com.nekomaster1000.infernalexp.init.IEBlocks;
import com.nekomaster1000.infernalexp.init.IEEffects;
import com.nekomaster1000.infernalexp.init.IEEntityTypes;
import com.nekomaster1000.infernalexp.init.IESoundEvents;

import net.minecraft.block.BlockState;
import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.CreatureAttribute;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.Pose;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.FlyingMovementController;
import net.minecraft.entity.ai.controller.MovementController;
import net.minecraft.entity.ai.goal.BreedGoal;
import net.minecraft.entity.ai.goal.EatGrassGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.HurtByTargetGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.IFlyingAnimal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.FlyingPathNavigator;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

// Extends AnimalEntity and implements IFlyingAnimal like BeeEntity class
public class GlowsquitoEntity extends AnimalEntity implements IFlyingAnimal {
	private static final DataParameter<Boolean> BRED = EntityDataManager.createKey(GlowsquitoEntity.class, DataSerializers.BOOLEAN);

	private static final Ingredient TEMPTATION_ITEMS = Ingredient.fromItems(IEBlocks.SHROOMLIGHT_FUNGUS.get().asItem());

	private EatGrassGoal eatGrassGoal;
	private int hogTimer;

	public GlowsquitoEntity(EntityType<? extends AnimalEntity> type, World worldIn) {
		super(type, worldIn);
		this.moveController = new FlyingMovementController(this, 20, true); // Flying entity
		this.setPathPriority(PathNodeType.DANGER_FIRE, -1.0F);
		this.setPathPriority(PathNodeType.WATER, -1.0F);
		this.setPathPriority(PathNodeType.WATER_BORDER, 16.0F);
	}

	// func_233666_p_ ---> registerAttributes()
	public static AttributeModifierMap.MutableAttribute setCustomAttributes() {
		return MobEntity.func_233666_p_().createMutableAttribute(Attributes.MAX_HEALTH, 32.0D).createMutableAttribute(Attributes.ATTACK_DAMAGE, 1.0D).createMutableAttribute(Attributes.FLYING_SPEED, 0.6D)
				// Required for flying entity, doesn't seem to affect actual movement speed
				.createMutableAttribute(Attributes.MOVEMENT_SPEED, 0.5D);
		// Turning this up makes them bounce on the ground like crazy, how do we fix
		// that?
	}

	public CreatureAttribute getCreatureAttribute() {
		return CreatureAttribute.ARTHROPOD;
	}

	@Override
	public GlowsquitoEntity createChild(ServerWorld world, AgeableEntity parent) {
		GlowsquitoEntity glowsquitoEntity = IEEntityTypes.GLOWSQUITO.get().create(world);
		glowsquitoEntity.setBred(true);

		return glowsquitoEntity;
	}

	public boolean isBreedingItem(ItemStack stack) {
		return TEMPTATION_ITEMS.test(stack);
	}

	protected void registerData() {
		super.registerData();
		this.dataManager.register(BRED, false);
	}

	public boolean getBred() {
		return this.dataManager.get(BRED);
	}

	public void setBred(boolean isBred) {
		this.dataManager.set(BRED, isBred);
	}

	public void writeAdditional(CompoundNBT compound) {
		super.writeAdditional(compound);
		compound.putBoolean("Bred", this.getBred());
	}

	/**
	 * (abstract) Protected helper method to read subclass entity data from NBT.
	 */
	public void readAdditional(CompoundNBT compound) {
		super.readAdditional(compound);
		this.setBred(compound.getBoolean("Bred"));
	}

	protected float getStandingEyeHeight(Pose poseIn, EntitySize sizeIn) {
		return this.isChild() ? sizeIn.height * 0.35F : sizeIn.height * 0.72F;
	}

	@Override
	protected void collideWithEntity(Entity entityIn) {
		super.collideWithEntity(entityIn);
		if (!this.isChild() && entityIn instanceof LivingEntity && !(entityIn instanceof GlowsquitoEntity)) {
			((LivingEntity) entityIn).addPotionEffect(new EffectInstance(IEEffects.LUMINOUS.get(), 200));
		}
	}

	protected PathNavigator createNavigator(World worldIn) {
		FlyingPathNavigator flyingpathnavigator = new FlyingPathNavigator(this, worldIn) {
			public boolean canEntityStandOnPos(BlockPos pos) {
				return !this.world.getBlockState(pos.down()).isAir();
			}

			public void tick() {
				super.tick();
			}
		};
		flyingpathnavigator.setCanOpenDoors(false);
		flyingpathnavigator.setCanSwim(false);
		flyingpathnavigator.setCanEnterDoors(true);
		return flyingpathnavigator;
	}

	// Entity won't take fall damage
	public boolean onLivingFall(float distance, float damageMultiplier) {
		return false;
	}

	static class LookAroundGoal extends Goal {
		private final GlowsquitoEntity parentEntity;

		public LookAroundGoal(GlowsquitoEntity ghast) {
			this.parentEntity = ghast;
			this.setMutexFlags(EnumSet.of(Goal.Flag.LOOK));
		}

		/**
		 * Returns whether execution should begin. You can also read and cache any state
		 * necessary for execution in this method as well.
		 */
		public boolean shouldExecute() {
			return true;
		}

		/**
		 * Keep ticking a continuous task that has already been started
		 */
		public void tick() {
			Vector3d vector3d = this.parentEntity.getMotion();
			this.parentEntity.rotationYaw = -((float) MathHelper.atan2(vector3d.x, vector3d.z)) * (180F / (float) Math.PI);
			this.parentEntity.renderYawOffset = this.parentEntity.rotationYaw;
		}
	}

	static class MoveHelperController extends MovementController {
		private final GlowsquitoEntity parentEntity;
		private int courseChangeCooldown;

		public MoveHelperController(GlowsquitoEntity ghast) {
			super(ghast);
			this.parentEntity = ghast;
		}

		public void tick() {
			if (this.action == MovementController.Action.MOVE_TO) {
				if (this.courseChangeCooldown-- <= 0) {
					this.courseChangeCooldown += this.parentEntity.getRNG().nextInt(5) + 2;
					Vector3d vector3d = new Vector3d(this.posX - this.parentEntity.getPosX(), this.posY - this.parentEntity.getPosY(), this.posZ - this.parentEntity.getPosZ());
					double d0 = vector3d.length();
					vector3d = vector3d.normalize();
					if (this.func_220673_a(vector3d, MathHelper.ceil(d0))) {
						this.parentEntity.setMotion(this.parentEntity.getMotion().add(vector3d.scale(0.1D)));
					} else {
						this.action = MovementController.Action.WAIT;
					}
				}

			}
		}

		private boolean func_220673_a(Vector3d vector3d, int p_220673_2_) {
			AxisAlignedBB axisalignedbb = this.parentEntity.getBoundingBox();

			for (int i = 1; i < p_220673_2_; ++i) {
				axisalignedbb = axisalignedbb.offset(vector3d);
				if (!this.parentEntity.world.hasNoCollisions(this.parentEntity, axisalignedbb)) {
					return false;
				}
			}

			return true;
		}
	}

	static class RandomFlyGoal extends Goal {
		private final GlowsquitoEntity parentEntity;

		public RandomFlyGoal(GlowsquitoEntity glowsquito) {
			this.parentEntity = glowsquito;
			this.setMutexFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		/**
		 * Returns whether execution should begin. You can also read and cache any state
		 * necessary for execution in this method as well.
		 */
		public boolean shouldExecute() {
			MovementController movementcontroller = this.parentEntity.getMoveHelper();
			if (!movementcontroller.isUpdating()) {
				return true;
			} else {
				double d0 = movementcontroller.getX() - this.parentEntity.getPosX();
				double d1 = movementcontroller.getY() - this.parentEntity.getPosY();
				double d2 = movementcontroller.getZ() - this.parentEntity.getPosZ();
				double d3 = d0 * d0 + d1 * d1 + d2 * d2;
				return d3 < 1.0D || d3 > 3600.0D;
			}
		}

		/**
		 * Returns whether an in-progress EntityAIBase should continue executing
		 */
		public boolean shouldContinueExecuting() {
			return false;
		}

		/**
		 * Execute a one shot task or start executing a continuous task
		 */
		public void startExecuting() {
			Random random = this.parentEntity.getRNG();
			double d0 = this.parentEntity.getPosX() + (double) ((random.nextFloat() * 2.0F - 1.0F) * 16.0F);
			double d1 = this.parentEntity.getPosY() + (double) ((random.nextFloat() * 2.0F - 1.0F) * 16.0F);
			double d2 = this.parentEntity.getPosZ() + (double) ((random.nextFloat() * 2.0F - 1.0F) * 16.0F);
			this.parentEntity.getMoveHelper().setMoveTo(d0, d1, d2, 1.0D);
		}
	}

	// Simple goal for wandering around. Modified from Vanilla's BeeEntity
	// WanderGoal subclass
	class WanderGoal extends Goal {
		WanderGoal() {
			this.setMutexFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		/**
		 * Returns whether execution should begin. You can also read and cache any state
		 * necessary for execution in this method as well.
		 */
		public boolean shouldExecute() {
			return GlowsquitoEntity.this.navigator.noPath() && GlowsquitoEntity.this.rand.nextInt(10) == 0;
		}

		/**
		 * Returns whether an in-progress EntityAIBase should continue executing
		 */
		public boolean shouldContinueExecuting() {
			return GlowsquitoEntity.this.navigator.hasPath();
		}

		/**
		 * Execute a one shot task or start executing a continuous task
		 */
		public void startExecuting() {
			Vector3d vector3d = this.getRandomLocation();
			if (vector3d != null) {
				GlowsquitoEntity.this.navigator.setPath(GlowsquitoEntity.this.navigator.getPathToPos(new BlockPos(vector3d), 1), 1.0D);
			}

		}

		@Nullable
		private Vector3d getRandomLocation() {
			Vector3d vector3d;
			vector3d = GlowsquitoEntity.this.getLook(0.0F);

			Vector3d vector3d2 = RandomPositionGenerator.findAirTarget(GlowsquitoEntity.this, 8, 7, vector3d, ((float) Math.PI / 2F), 2, 1);
			return vector3d2 != null ? vector3d2 : RandomPositionGenerator.findGroundTarget(GlowsquitoEntity.this, 8, 4, -2, vector3d, (float) Math.PI / 2F);
		}
	}

	// GOALS
	@Override
	protected void registerGoals() {
		super.registerGoals();

		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 0.6D, true));

		this.eatGrassGoal = new EatGrassGoal(this);

		this.goalSelector.addGoal(0, new MeleeAttackGoal(this, 0.8D, true));
		this.goalSelector.addGoal(1, new MoveTowardsTargetGoal(this, 0.8D, 32.0F));
		// this.goalSelector.addGoal(5, new GlowsquitoEntity.RandomFlyGoal(this));
		this.goalSelector.addGoal(2, new BreedGoal(this, 0.8d));
		this.goalSelector.addGoal(3, new TemptGoal(this, 0.8d, false, TEMPTATION_ITEMS));
		this.goalSelector.addGoal(8, new GlowsquitoEntity.WanderGoal());
		// this.goalSelector.addGoal(7, new GlowsquitoEntity.LookAroundGoal(this));
		// this.goalSelector.addGoal(5, this.eatGrassGoal);
		this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(1, new TargetWithEffectGoal(this, CreatureEntity.class, true, false, IEEffects.LUMINOUS.get(), GlowsquitoEntity.class));
		this.targetSelector.addGoal(1, new TargetWithEffectGoal(this, MonsterEntity.class, true, false, IEEffects.LUMINOUS.get(), GlowsquitoEntity.class));
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, BlackstoneDwarfEntity.class, true));
	}

    public boolean isImmuneToFire() {
        return true;
    }

	@Override
	protected int getExperiencePoints(PlayerEntity player) {
		return 1 + this.world.rand.nextInt(4);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return null;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return IESoundEvents.GLOWSQUITO_DEATH.get();
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
		return IESoundEvents.GLOWSQUITO_HURT.get();
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState blockIn) {
		this.playSound(SoundEvents.ENTITY_PIG_STEP, 0.15F, 1.0F);
	}

	public boolean attackEntityAsMob(Entity entityIn) {
		if (!super.attackEntityAsMob(entityIn)) {
			return false;
		} else {
			if (entityIn instanceof LivingEntity) {
				((LivingEntity) entityIn).addPotionEffect(new EffectInstance(IEEffects.LUMINOUS.get(), 600, 0, true, true)); // 30s
				((LivingEntity) entityIn).addPotionEffect(new EffectInstance(Effects.POISON, 100)); // 5s
			}

			return true;
		}
	}

	@Override
	protected void updateAITasks() {
		this.hogTimer = this.eatGrassGoal.getEatingGrassTimer();
		super.updateAITasks();
	}

	@Override
	public void livingTick() {
		super.livingTick();
		if (this.world.isRemote) {
			this.hogTimer = Math.max(0, this.hogTimer - 1);
		}
	}

	@OnlyIn(Dist.CLIENT)
	public void handleStatusUpdate(byte id) {
		if (id == 10) {
			this.hogTimer = 40;
		} else {
			super.handleStatusUpdate(id);
		}
	}

}
