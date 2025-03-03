package de.dafuqs.spectrum.blocks.fusion_shrine;

import de.dafuqs.spectrum.InventoryHelper;
import de.dafuqs.spectrum.progression.SpectrumAdvancementCriteria;
import de.dafuqs.spectrum.registries.SpectrumBlockEntityRegistry;
import de.dafuqs.spectrum.registries.SpectrumMultiblocks;
import net.fabricmc.fabric.mixin.transfer.BucketItemAccessor;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import vazkii.patchouli.api.IMultiblock;
import vazkii.patchouli.api.PatchouliAPI;

import java.util.Optional;

public class FusionShrineBlock extends BlockWithEntity {

	public static final IntProperty LIGHT_LEVEL = IntProperty.of("light_level", 0, 15);
	protected static final VoxelShape SHAPE = Block.createCuboidShape(2.0D, 0.0D, 2.0D, 14.0D, 15.0D, 14.0D);

	public FusionShrineBlock(Settings settings) {
		super(settings);
		setDefaultState(getStateManager().getDefaultState().with(LIGHT_LEVEL, 0));
	}
	
	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(LIGHT_LEVEL);
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new FusionShrineBlockEntity(pos, state);
	}
	
	@Override
	public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
		return false;
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if(world.isClient) {
			return ActionResult.SUCCESS;
		} else {
			ItemStack itemStack = player.getStackInHand(hand);
			boolean itemsChanged = false;
			Optional<SoundEvent> soundToPlay = Optional.empty();
			
			BlockEntity blockEntity = world.getBlockEntity(pos);
			if(blockEntity instanceof FusionShrineBlockEntity fusionShrineBlockEntity) {
				if (itemStack.getItem() instanceof BucketItem) {
					fusionShrineBlockEntity.setOwner(player);
					
					Fluid storedFluid = fusionShrineBlockEntity.getFluid();
					Fluid bucketFluid = ((BucketItemAccessor) itemStack.getItem()).fabric_getFluid();
					if (storedFluid == Fluids.EMPTY && bucketFluid != Fluids.EMPTY) {
						fusionShrineBlockEntity.setFluid(bucketFluid);
						if (!player.isCreative()) {
							player.setStackInHand(hand, new ItemStack(Items.BUCKET));
						}
						
						soundToPlay = bucketFluid.getBucketFillSound();
						itemsChanged = true;
					} else if (storedFluid != Fluids.EMPTY && bucketFluid == Fluids.EMPTY) {
						fusionShrineBlockEntity.setFluid(Fluids.EMPTY);
						world.setBlockState(pos, world.getBlockState(pos).with(LIGHT_LEVEL, 0));
						if (!player.isCreative()) {
							player.setStackInHand(hand, new ItemStack(storedFluid.getBucketItem()));
						}
						
						soundToPlay = storedFluid.getBucketFillSound();
						itemsChanged = true;
					}
				} else {
					// if the structure is valid the player can put / retrieve blocks into the shrine
					if (player.isSneaking()) {
						ItemStack retrievedStack = ItemStack.EMPTY;
						Inventory inventory = fusionShrineBlockEntity.getInventory();
						for (int i = inventory.size() - 1; i >= 0; i--) {
							retrievedStack = inventory.removeStack(i);
							if (!retrievedStack.isEmpty()) {
								break;
							}
						}
						if (!retrievedStack.isEmpty()) {
							player.giveItemStack(retrievedStack);
							soundToPlay = Optional.of(SoundEvents.ENTITY_ITEM_PICKUP);
							itemsChanged = true;
						}
					} else if (!itemStack.isEmpty() && verifyStructure(world, pos, (ServerPlayerEntity) player)) {
						fusionShrineBlockEntity.setOwner(player);
						
						ItemStack remainingStack = InventoryHelper.addToInventory(itemStack, fusionShrineBlockEntity.getInventory(), null);
						player.setStackInHand(hand, remainingStack);
						
						soundToPlay = Optional.of(SoundEvents.ENTITY_ITEM_PICKUP);
						itemsChanged = true;
					}
				}
				
				if(itemsChanged) {
					fusionShrineBlockEntity.updateInClientWorld();
					if(soundToPlay.isPresent()) {
						world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.8F, 0.8F + world.random.nextFloat() * 0.6F);
					}
				}
			}
			
			return ActionResult.CONSUME;
		}
	}
	
	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		if(world.isClient) {
			return checkType(type, SpectrumBlockEntityRegistry.FUSION_SHRINE, FusionShrineBlockEntity::clientTick);
		} else {
			return checkType(type, SpectrumBlockEntityRegistry.FUSION_SHRINE, FusionShrineBlockEntity::serverTick);
		}
	}

	public static boolean verifyStructure(World world, BlockPos blockPos, @Nullable ServerPlayerEntity serverPlayerEntity) {
		IMultiblock multiblock = SpectrumMultiblocks.MULTIBLOCKS.get(SpectrumMultiblocks.FUSION_SHRINE_IDENTIFIER);
		boolean valid = multiblock.validate(world, blockPos.down(), BlockRotation.NONE);

		if(valid) {
			if(serverPlayerEntity != null) {
				SpectrumAdvancementCriteria.COMPLETED_MULTIBLOCK.trigger(serverPlayerEntity, multiblock);
			}
		} else {
			IMultiblock currentMultiBlock = PatchouliAPI.get().getCurrentMultiblock();
			if(currentMultiBlock == multiblock) {
				PatchouliAPI.get().clearMultiblock();
			} else {
				PatchouliAPI.get().showMultiblock(multiblock, new TranslatableText("multiblock.spectrum.fusion_shrine.structure"), blockPos.down(2), BlockRotation.NONE);
				scatterContents(world, blockPos);
			}
		}

		return valid;
	}

	// drop all currently stored items
	@Override
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if(!newState.getBlock().equals(this)) { // happens when filling with fluid
			scatterContents(world, pos);
			IMultiblock currentMultiBlock = PatchouliAPI.get().getCurrentMultiblock();
			if(currentMultiBlock != null) {
				if (currentMultiBlock.getID().equals(SpectrumMultiblocks.FUSION_SHRINE_IDENTIFIER)) {
					PatchouliAPI.get().clearMultiblock();
				}
			}
		}
		super.onStateReplaced(state, world, pos, newState, moved);
	}

	public static void scatterContents(World world, BlockPos pos) {
		Block block = world.getBlockState(pos).getBlock();
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity instanceof FusionShrineBlockEntity fusionShrineBlockEntity) {
			ItemScatterer.spawn(world, pos, fusionShrineBlockEntity.getInventory());
			world.updateComparators(pos, block);
		}
	}

}
