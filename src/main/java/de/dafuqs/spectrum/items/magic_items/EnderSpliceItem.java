package de.dafuqs.spectrum.items.magic_items;

import de.dafuqs.spectrum.SpectrumClient;
import de.dafuqs.spectrum.Support;
import de.dafuqs.spectrum.interfaces.PlayerOwned;
import de.dafuqs.spectrum.sound.EnderSpliceChargingSoundInstance;
import de.dafuqs.spectrum.sound.SpectrumSoundEvents;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.*;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EnderSpliceItem extends Item {
	
	public EnderSpliceItem(Settings settings) {
		super(settings);
	}

	@Override
	public ItemStack finishUsing(ItemStack itemStack, World world, LivingEntity user) {
		if(world.isClient) {
			if(getTeleportTargetPos(itemStack).isEmpty() && getTeleportTargetPlayerUUID(itemStack).isEmpty()) {
				// If aiming at an entity: trigger entity interaction
				HitResult hitResult = MinecraftClient.getInstance().crosshairTarget;
				if(hitResult.getType() == HitResult.Type.ENTITY) {
					EntityHitResult entityHitResult = (EntityHitResult) hitResult;
					MinecraftClient.getInstance().interactionManager.interactEntity(MinecraftClient.getInstance().player, entityHitResult.getEntity(), user.getActiveHand());
				}
			}
		} else {
			PlayerEntity playerEntity = user instanceof PlayerEntity ? (PlayerEntity) user : null;
			if (playerEntity instanceof ServerPlayerEntity) {
				Criteria.CONSUME_ITEM.trigger((ServerPlayerEntity) playerEntity, itemStack);
			}
			
			// If Dimension & Pos stored => Teleport to that position
			Optional<Pair<String, Vec3d>> teleportTargetPos = getTeleportTargetPos(itemStack);
			if (teleportTargetPos.isPresent()) {
				RegistryKey<World> targetWorldKey = RegistryKey.of(Registry.WORLD_KEY, new Identifier(teleportTargetPos.get().getLeft()));
				World targetWorld = world.getServer().getWorld(targetWorldKey);
				teleportPlayerToPos(world, user, playerEntity, targetWorld, teleportTargetPos.get().getRight());
			} else {
				// If UUID stored => Teleport to player, if online
				Optional<UUID> teleportTargetPlayerUUID = getTeleportTargetPlayerUUID(itemStack);
				if (teleportTargetPlayerUUID.isPresent()) {
					teleportPlayerToPlayerWithUUID(world, user, playerEntity, teleportTargetPlayerUUID.get());
				} else {
					// Nothing stored => Store current position
					setTeleportTargetPos(itemStack, playerEntity.getEntityWorld(), playerEntity.getPos());
					((ServerPlayerEntity) playerEntity).networkHandler.sendPacket(new PlaySoundIdS2CPacket(SpectrumSoundEvents.ENDER_SPLICE_BOUND.getId(), SoundCategory.PLAYERS, playerEntity.getPos(), 1.0F, 1.0F));
				}
			}
			
			playerEntity.incrementStat(Stats.USED.getOrCreateStat(this));
			if (!playerEntity.getAbilities().creativeMode) {
				itemStack.decrement(1);
			}
		}
		
		return itemStack;
	}
	
	private void teleportPlayerToPlayerWithUUID(World world, LivingEntity user, PlayerEntity playerEntity, UUID targetPlayerUUID) {
		PlayerEntity targetPlayer = PlayerOwned.getPlayerEntityIfOnline(world, targetPlayerUUID);
		if(targetPlayer != null) {
			teleportPlayerToPos(targetPlayer.getEntityWorld(), user, playerEntity, targetPlayer.getEntityWorld(), targetPlayer.getPos());
		}
	}
	
	private void teleportPlayerToPos(World world, LivingEntity user, PlayerEntity playerEntity, World targetWorld, Vec3d targetPos) {
		if (user.getEntityWorld().getRegistryKey().getValue().toString().equals(targetWorld.getRegistryKey().getValue().toString())) {
			Vec3d currentPos = playerEntity.getPos();
			world.playSound(playerEntity, currentPos.getX(), currentPos.getY(), currentPos.getZ(), SpectrumSoundEvents.PLAYER_TELEPORTS, SoundCategory.PLAYERS, 1.0F, 1.0F);
			
			user.requestTeleport(targetPos.getX(), targetPos.y + 0.25, targetPos.z); // +0.25 makes it look way more lively
			world.playSound(playerEntity, targetPos.getX(), targetPos.y, targetPos.z, SpectrumSoundEvents.PLAYER_TELEPORTS, SoundCategory.PLAYERS, 1.0F, 1.0F);
			
			// make sure the sound plays even when the player currently teleports
			if(playerEntity instanceof ServerPlayerEntity) {
				((ServerPlayerEntity) playerEntity).networkHandler.sendPacket(new PlaySoundIdS2CPacket(SpectrumSoundEvents.PLAYER_TELEPORTS.getId(), SoundCategory.PLAYERS, playerEntity.getPos(), 1.0F, 1.0F));
				((ServerPlayerEntity) playerEntity).networkHandler.sendPacket(new PlaySoundIdS2CPacket(SoundEvents.BLOCK_GLASS_BREAK.getId(), SoundCategory.PLAYERS, playerEntity.getPos(), 1.0F, 1.0F));
			}
		}
	}
	
	@Override
	public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
		if(user.getItemUseTime() >= getMaxUseTime(stack)) { // only true when triggered via onFinishUsing()
			if (entity instanceof PlayerEntity && getTeleportTargetPlayerUUID(stack).isEmpty()) {
				if (entity instanceof ServerPlayerEntity serverPlayerEntity) {
					setTeleportTargetPlayer(stack, serverPlayerEntity);
					((ServerPlayerEntity) user).networkHandler.sendPacket(new PlaySoundIdS2CPacket(SpectrumSoundEvents.ENDER_SPLICE_BOUND.getId(), SoundCategory.PLAYERS, user.getPos(), 1.0F, 1.0F));
					serverPlayerEntity.networkHandler.sendPacket(new PlaySoundIdS2CPacket(SpectrumSoundEvents.ENDER_SPLICE_BOUND.getId(), SoundCategory.PLAYERS, user.getPos(), 1.0F, 1.0F));
				}
				user.stopUsingItem();
			}
		}
		
		return ActionResult.SUCCESS;
	}
	
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		if(world.isClient) {
			startSoundInstance(user);
		}
		return ItemUsage.consumeHeldItem(world, user, hand);
	}

	@Environment(EnvType.CLIENT)
	public void startSoundInstance(PlayerEntity user) {
		SpectrumClient.minecraftClient.getSoundManager().play(new EnderSpliceChargingSoundInstance(user));
	}

	public int getMaxUseTime(ItemStack stack) {
		return 48;
	}

	public UseAction getUseAction(ItemStack stack) {
		return UseAction.BLOCK;
	}

	@Environment(EnvType.CLIENT)
	public void appendTooltip(ItemStack itemStack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		// If Dimension & Pos stored => Teleport to that position
		Optional<Pair<String, Vec3d>> teleportTargetPos = getTeleportTargetPos(itemStack);
		if (teleportTargetPos.isPresent()) {
			String dimensionDisplayString = Support.getReadableDimensionString(teleportTargetPos.get().getLeft());
			Vec3d pos = teleportTargetPos.get().getRight();
			tooltip.add(new TranslatableText("item.spectrum.ender_splice.tooltip.bound_pos", (int) pos.x, (int) pos.y, (int) pos.z, dimensionDisplayString));
			return;
		} else {
			// If UUID stored => Teleport to player, if online
			Optional<UUID> teleportTargetPlayerUUID = getTeleportTargetPlayerUUID(itemStack);
			if(teleportTargetPlayerUUID.isPresent()) {
				Optional<String> teleportTargetPlayerName = getTeleportTargetPlayerName(itemStack);
				if(teleportTargetPlayerName.isPresent()) {
					tooltip.add(new TranslatableText("item.spectrum.ender_splice.tooltip.bound_player", teleportTargetPlayerName.get()));
				} else {
					tooltip.add(new TranslatableText("item.spectrum.ender_splice.tooltip.bound_player", "???"));
				}
				return;
			}
		}
		
		tooltip.add(new TranslatableText("item.spectrum.ender_splice.tooltip.unbound"));
	}
	
	public void setTeleportTargetPos(@NotNull ItemStack itemStack, World world, Vec3d pos) {
		NbtCompound nbtCompound = itemStack.getOrCreateNbt();
		
		// Remove player tags, if present
		if(nbtCompound.contains("TargetPlayerName")) { nbtCompound.remove("TargetPlayerName"); }
		if(nbtCompound.contains("TargetPlayerUUID")) { nbtCompound.remove("TargetPlayerUUID"); }
		
		// Add pos
		nbtCompound.putDouble("PosX", pos.getX());
		nbtCompound.putDouble("PosY", pos.getY());
		nbtCompound.putDouble("PosZ", pos.getZ());
		nbtCompound.putString("Dimension", world.getRegistryKey().getValue().toString());
		itemStack.setNbt(nbtCompound);
	}
	
	public void setTeleportTargetPlayer(@NotNull ItemStack itemStack, ServerPlayerEntity player) {
		NbtCompound nbtCompound = itemStack.getOrCreateNbt();
		
		// Override target pos, if present
		if(nbtCompound.contains("PosX")) { nbtCompound.remove("PosX"); }
		if(nbtCompound.contains("PosY")) { nbtCompound.remove("PosY"); }
		if(nbtCompound.contains("PosZ")) { nbtCompound.remove("PosZ"); }
		if(nbtCompound.contains("Dimension")) { nbtCompound.remove("Dimension"); }
		
		// Add player
		nbtCompound.putString("TargetPlayerName", player.getName().asString());
		nbtCompound.putUuid("TargetPlayerUUID", player.getUuid());
		itemStack.setNbt(nbtCompound);
	}

	public Optional<Pair<String, Vec3d>> getTeleportTargetPos(@NotNull ItemStack itemStack) {
		NbtCompound nbtCompound = itemStack.getNbt();
		if(nbtCompound != null && nbtCompound.contains("PosX") && nbtCompound.contains("PosY") && nbtCompound.contains("PosZ") && nbtCompound.contains("Dimension")) {
			String dimensionKeyString = nbtCompound.getString("Dimension");
			double x = nbtCompound.getDouble("PosX");
			double y = nbtCompound.getDouble("PosY");
			double z = nbtCompound.getDouble("PosZ");
			Vec3d pos = new Vec3d(x, y, z);
			
			return Optional.of(new Pair<>(dimensionKeyString, pos));
		}
		return Optional.empty();
	}
	
	public Optional<UUID> getTeleportTargetPlayerUUID(@NotNull ItemStack itemStack) {
		NbtCompound nbtCompound = itemStack.getNbt();
		if(nbtCompound != null && nbtCompound.contains("TargetPlayerUUID")) {
			return Optional.of(nbtCompound.getUuid("TargetPlayerUUID"));
		}
		return Optional.empty();
	}
	
	public Optional<String> getTeleportTargetPlayerName(@NotNull ItemStack itemStack) {
		NbtCompound nbtCompound = itemStack.getNbt();
		if(nbtCompound != null && nbtCompound.contains("TargetPlayerName")) {
			return Optional.of(nbtCompound.getString("TargetPlayerName"));
		}
		return Optional.empty();
	}

}
