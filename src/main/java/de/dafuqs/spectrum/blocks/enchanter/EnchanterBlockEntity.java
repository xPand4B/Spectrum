package de.dafuqs.spectrum.blocks.enchanter;

import de.dafuqs.spectrum.ExperienceHelper;
import de.dafuqs.spectrum.Support;
import de.dafuqs.spectrum.blocks.fusion_shrine.FusionShrineBlock;
import de.dafuqs.spectrum.blocks.fusion_shrine.FusionShrineBlockEntity;
import de.dafuqs.spectrum.blocks.item_bowl.ItemBowlBlockEntity;
import de.dafuqs.spectrum.blocks.upgrade.Upgradeable;
import de.dafuqs.spectrum.interfaces.PlayerOwned;
import de.dafuqs.spectrum.items.ExperienceStorageItem;
import de.dafuqs.spectrum.items.magic_items.KnowledgeDropItem;
import de.dafuqs.spectrum.networking.SpectrumS2CPackets;
import de.dafuqs.spectrum.particle.SpectrumParticleTypes;
import de.dafuqs.spectrum.progression.SpectrumAdvancementCriteria;
import de.dafuqs.spectrum.recipe.SpectrumRecipeTypes;
import de.dafuqs.spectrum.recipe.enchanter.EnchanterRecipe;
import de.dafuqs.spectrum.recipe.enchantment_upgrade.EnchantmentUpgradeRecipe;
import de.dafuqs.spectrum.recipe.fusion_shrine.FusionShrineRecipe;
import de.dafuqs.spectrum.registries.SpectrumBlockEntityRegistry;
import de.dafuqs.spectrum.registries.SpectrumItems;
import de.dafuqs.spectrum.sound.SpectrumSoundEvents;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.advancement.AdvancementManager;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.EnchantingTableBlockEntity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.KnowledgeBookItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.Recipe;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EnchanterBlockEntity extends BlockEntity implements PlayerOwned, Upgradeable {
	
	public static final int INVENTORY_SIZE = 2; // 0: any itemstack, 1: Knowledge Drop;
	
	private UUID ownerUUID;
	protected SimpleInventory inventory;
	
	// since the item bowls around the enchanter hold some items themselves
	// they get cached here for faster recipe lookup
	// virtualInventoryRecipeOrientation is the order the items are ordered for the recipe to match (rotations form 0-3)
	protected SimpleInventory virtualInventoryIncludingBowlStacks;
	protected int virtualInventoryRecipeOrientation;
	
	protected boolean inventoryChanged;
	private Map<Upgradeable.UpgradeType, Double> upgrades;
	
	private Recipe currentRecipe;
	private int craftingTime;
	private int craftingTimeTotal;
	
	@Nullable
	private Direction itemFacing; // for rendering the item on the enchanter only
	
	public EnchanterBlockEntity(BlockPos pos, BlockState state) {
		super(SpectrumBlockEntityRegistry.ENCHANTER, pos, state);
		this.inventory = new SimpleInventory(INVENTORY_SIZE);
		this.virtualInventoryIncludingBowlStacks = new SimpleInventory(INVENTORY_SIZE + 8);
	}
	
	public void readNbt(NbtCompound nbt) {
		super.readNbt(nbt);
		this.inventory = new SimpleInventory(INVENTORY_SIZE);
		this.inventory.readNbtList(nbt.getList("inventory", 10));
		this.virtualInventoryIncludingBowlStacks = new SimpleInventory(INVENTORY_SIZE + 8);
		this.virtualInventoryIncludingBowlStacks.readNbtList(nbt.getList("virtual_inventory", 10));
		if(nbt.contains("item_facing", NbtElement.STRING_TYPE)) {
			this.itemFacing = Direction.valueOf(nbt.getString("item_facing").toUpperCase(Locale.ROOT));
		}
		if(nbt.contains("OwnerUUID")) {
			this.ownerUUID = nbt.getUuid("OwnerUUID");
		} else {
			this.ownerUUID = null;
		}
		if(nbt.contains("CurrentRecipe")) {
			String recipeString = nbt.getString("CurrentRecipe");
			if(!recipeString.isEmpty()) {
				Optional<? extends Recipe> optionalRecipe = Optional.empty();
				if (world != null) {
					optionalRecipe = world.getRecipeManager().get(new Identifier(recipeString));
				}
				if(optionalRecipe.isPresent() && optionalRecipe.get() instanceof FusionShrineRecipe optionalFusionRecipe) {
					this.currentRecipe = optionalFusionRecipe;
				} else {
					this.currentRecipe = null;
				}
			} else {
				this.currentRecipe = null;
			}
		} else {
			this.currentRecipe = null;
		}
		if(nbt.contains("Upgrades", NbtElement.COMPOUND_TYPE)) {
			this.upgrades = Upgradeable.fromNbt(nbt.getList("Upgrades", NbtElement.COMPOUND_TYPE));
		}
	}
	
	public void writeNbt(NbtCompound nbt) {
		super.writeNbt(nbt);
		nbt.put("inventory", this.inventory.toNbtList());
		nbt.put("virtual_inventory", this.virtualInventoryIncludingBowlStacks.toNbtList());
		if(this.itemFacing != null) {
			nbt.putString("item_facing", this.itemFacing.toString().toUpperCase(Locale.ROOT));
		}
		if(this.upgrades != null) {
			nbt.put("Upgrades", Upgradeable.toNbt(this.upgrades));
		}
		if(this.ownerUUID != null) {
			nbt.putUuid("OwnerUUID", this.ownerUUID);
		}
		if(this.currentRecipe != null) {
			nbt.putString("CurrentRecipe", this.currentRecipe.getId().toString());
		}
	}
	
	// Called when the chunk is first loaded to initialize this be
	public NbtCompound toInitialChunkDataNbt() {
		NbtCompound nbtCompound = new NbtCompound();
		this.writeNbt(nbtCompound);
		return nbtCompound;
	}
	
	@Nullable
	@Override
	public Packet<ClientPlayPacketListener> toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}
	
	public void updateInClientWorld() {
		world.updateListeners(pos, world.getBlockState(pos), world.getBlockState(pos), Block.NO_REDRAW);
	}
	
	public Inventory getInventory() {
		return this.inventory;
	}
	
	public void setItemFacingDirection(Direction facingDirection) {
		this.itemFacing = facingDirection;
	}
	
	public Direction getItemFacingDirection() {
		// if placed via pipe or other sources
		return Objects.requireNonNullElse(this.itemFacing, Direction.NORTH);
	}
	
	public static void clientTick(World world, BlockPos blockPos, BlockState blockState, EnchanterBlockEntity enchanterBlockEntity) {
		ItemStack experienceStack = enchanterBlockEntity.getInventory().getStack(1);
		if(!experienceStack.isEmpty() && experienceStack.getItem() instanceof ExperienceStorageItem) {
			int experience = ExperienceStorageItem.getStoredExperience(experienceStack);
			int amount = Support.getExperienceOrbSizeForExperience(experience);
			
			if(world.random.nextInt(10) < amount) {
				float randomX = 0.2F + world.getRandom().nextFloat() * 0.6F;
				float randomZ = 0.2F + world.getRandom().nextFloat() * 0.6F;
				float randomY = -0.1F + world.getRandom().nextFloat() * 0.4F;
				world.addParticle(SpectrumParticleTypes.LIME_SPARKLE_RISING, blockPos.getX() + randomX, blockPos.getY() + 2.5 + randomY, blockPos.getZ() + randomZ, 0.0D, -0.1D, 0.0D);
			}
		}
	}
	
	public static void serverTick(World world, BlockPos blockPos, BlockState blockState, EnchanterBlockEntity enchanterBlockEntity) {
		if(enchanterBlockEntity.upgrades == null) {
			enchanterBlockEntity.calculateUpgrades();
		}
		
		if(enchanterBlockEntity.inventoryChanged) {
			calculateCurrentRecipe(world, enchanterBlockEntity);
			enchanterBlockEntity.inventoryChanged = false;
		}
		
		boolean craftingSuccess = false;
		
		if(enchanterBlockEntity.currentRecipe != null && enchanterBlockEntity.craftingTime % 60 == 1) {
			PlayerEntity lastInteractedPlayer = PlayerOwned.getPlayerEntityIfOnline(world, enchanterBlockEntity.ownerUUID);
			
			boolean playerHasAdvancement = true;
			if(enchanterBlockEntity.currentRecipe instanceof EnchanterRecipe enchanterRecipe) {
				playerHasAdvancement = Support.hasAdvancement(lastInteractedPlayer, enchanterRecipe.getRequiredAdvancementIdentifier());
			} else if(enchanterBlockEntity.currentRecipe instanceof EnchantmentUpgradeRecipe enchantmentUpgradeRecipe) {
				playerHasAdvancement = Support.hasAdvancement(lastInteractedPlayer, enchantmentUpgradeRecipe.getRequiredAdvancementIdentifier());
			}
			boolean structureComplete = EnchanterBlock.verifyStructure(world, blockPos, null);
			
			if(!playerHasAdvancement || !structureComplete) {
				if(!structureComplete) {
					world.playSound(null, enchanterBlockEntity.getPos(), SpectrumSoundEvents.FUSION_SHRINE_CRAFTING_ABORTED, SoundCategory.BLOCKS, 0.9F + enchanterBlockEntity.world.random.nextFloat() * 0.2F, 0.9F + enchanterBlockEntity.world.random.nextFloat() * 0.2F);
					world.playSound(null, enchanterBlockEntity.getPos(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.9F + enchanterBlockEntity.world.random.nextFloat() * 0.2F, 0.5F + enchanterBlockEntity.world.random.nextFloat() * 0.2F);
					EnchanterBlock.scatterContents(world, blockPos);
				}
				enchanterBlockEntity.craftingTime = 0;
				return;
			}
		}
		
		if (enchanterBlockEntity.currentRecipe instanceof EnchanterRecipe enchanterRecipe) {
			if(enchanterRecipe.getRequiredExperience() == 0 || ExperienceStorageItem.getStoredExperience(enchanterBlockEntity.inventory.getStack(1)) >= enchanterRecipe.getRequiredExperience()) {
				enchanterBlockEntity.craftingTime++;
				// TODO: Play particles
				// TODO: Sounds
				if (enchanterBlockEntity.craftingTime == enchanterBlockEntity.craftingTimeTotal) {
					craftEnchanterRecipe(world, enchanterBlockEntity, enchanterRecipe);
					// TODO: Play particles
					// TODO: Sounds
					craftingSuccess = true;
				}
			} else {
				// Missing required experience
				// TODO: Play particles
				// TODO: Sounds
			}
		} else if (enchanterBlockEntity.currentRecipe instanceof EnchantmentUpgradeRecipe enchantmentUpgradeRecipe) {
			int consumedItems = tickEnchantmentUpgradeRecipe(world, enchanterBlockEntity, enchantmentUpgradeRecipe);
			// TODO: Play particles
			// TODO: Sounds
			enchanterBlockEntity.craftingTime += consumedItems;
			if(enchanterBlockEntity.craftingTime >= enchanterBlockEntity.craftingTimeTotal) {
				// TODO: Implementation
				// TODO: Play particles
				// TODO: Sounds
				craftEnchantmentUpgradeRecipe(world, enchanterBlockEntity, enchantmentUpgradeRecipe);
				craftingSuccess = true;
			}
		} else {
			// in-code recipe for item + books => enchanted item
			// TODO: Implementation
			// TODO: Play particles
			// TODO: Sounds
		}
		
		if(craftingSuccess) {
			enchanterBlockEntity.inventoryChanged();
		}
	}
	
	public static void craftEnchanterRecipe(World world, @NotNull EnchanterBlockEntity enchanterBlockEntity, @NotNull EnchanterRecipe enchanterRecipe) {
		ItemStack knowledgeDropStack = enchanterBlockEntity.getInventory().getStack(1);
		if(knowledgeDropStack.isOf(SpectrumItems.KNOWLEDGE_DROP)) {
			ExperienceStorageItem.removeStoredExperience(knowledgeDropStack, enchanterRecipe.getRequiredExperience());
		}
		
		for(int i = 0; i < 8; i++) {
			// since this recipe uses 1 item in each slot we can just iterate them all and decrement with 1
			BlockPos itemBowlPos = enchanterBlockEntity.pos.add(getItemBowlPositionOffset(i, enchanterBlockEntity.virtualInventoryRecipeOrientation));
			BlockEntity blockEntity = world.getBlockEntity(itemBowlPos);
			if(blockEntity instanceof ItemBowlBlockEntity itemBowlBlockEntity) {
				ItemStack itemStack = itemBowlBlockEntity.getInventory().getStack(0);
				Item recipeRemainderItem = itemStack.getItem().getRecipeRemainder();
				if(recipeRemainderItem != null) {
					itemBowlBlockEntity.getInventory().setStack(0, recipeRemainderItem.getDefaultStack());
				} else if(itemStack.getCount() == 1) {
					itemBowlBlockEntity.getInventory().setStack(0, ItemStack.EMPTY);
				} else {
					itemBowlBlockEntity.getInventory().getStack(0).decrement(1);
				}
				itemBowlBlockEntity.updateInClientWorld();
			}
		}
		
		ItemStack resultStack = enchanterRecipe.getOutput().copy();
		enchanterBlockEntity.getInventory().setStack(0, resultStack);
		
		int spentLevels = ExperienceHelper.getLevelForExperience(enchanterRecipe.getRequiredExperience());
		grantPlayerEnchantingAdvancement(world, enchanterBlockEntity.ownerUUID, resultStack, spentLevels);
	}
	
	public static int tickEnchantmentUpgradeRecipe(World world, @NotNull EnchanterBlockEntity enchanterBlockEntity, @NotNull EnchantmentUpgradeRecipe enchantmentUpgradeRecipe) {
		// TODO
		return 0;
	}
	
	public static void craftEnchantmentUpgradeRecipe(World world, @NotNull EnchanterBlockEntity enchanterBlockEntity, @NotNull EnchantmentUpgradeRecipe enchantmentUpgradeRecipe) {
		ItemStack knowledgeDropStack = enchanterBlockEntity.getInventory().getStack(1);
		if(knowledgeDropStack.isOf(SpectrumItems.KNOWLEDGE_DROP)) {
			ExperienceStorageItem.removeStoredExperience(knowledgeDropStack, enchantmentUpgradeRecipe.getRequiredExperience());
		}
		
		// TODO: decrement item bowl contents
		
		ItemStack resultStack = enchantmentUpgradeRecipe.getOutput().copy();
		enchanterBlockEntity.getInventory().setStack(0, resultStack);
		
		int spentLevels = ExperienceHelper.getLevelForExperience(enchantmentUpgradeRecipe.getRequiredExperience());
		grantPlayerEnchantingAdvancement(world, enchanterBlockEntity.ownerUUID, resultStack, spentLevels);
	}
	
	public static Vec3i getItemBowlPositionOffset(int index, int orientation) {
		List<Vec3i> pos = new ArrayList<>();
		pos.add(new Vec3i(5, 0, -3));
		pos.add(new Vec3i(5, 0, 3));
		pos.add(new Vec3i(3, 0, 5));
		pos.add(new Vec3i(-3, 0, 5));
		pos.add(new Vec3i(-5, 0, 3));
		pos.add(new Vec3i(-5, 0, -3));
		pos.add(new Vec3i(-3, 0, -5));
		pos.add(new Vec3i(3, 0, -5));
		
		int offset = (orientation * 2 + index) % 8;
		return pos.get(offset);
	}
	
	/**
	 * Calculates and sets a new recipe for the enchanter based on it's inventory
	 * @param world The Enchanter World
	 * @param enchanterBlockEntity The Enchanter Block Entity
	 * @return Wether or not the previous recipe was still valid. False means it was changed
	 */
	private static boolean calculateCurrentRecipe(@NotNull World world, EnchanterBlockEntity enchanterBlockEntity) {
		if(enchanterBlockEntity.currentRecipe != null) {
			if(enchanterBlockEntity.currentRecipe.matches(enchanterBlockEntity.virtualInventoryIncludingBowlStacks, world)) {
				return true;
			}
		}
		
		enchanterBlockEntity.craftingTime = 0;
		Recipe previousRecipe = enchanterBlockEntity.currentRecipe;
		
		SimpleInventory recipeTestInventory = new SimpleInventory(enchanterBlockEntity.virtualInventoryIncludingBowlStacks.size());
		recipeTestInventory.setStack(0, enchanterBlockEntity.virtualInventoryIncludingBowlStacks.getStack(0));
		recipeTestInventory.setStack(1, enchanterBlockEntity.virtualInventoryIncludingBowlStacks.getStack(1));
		// Cycle through 4 phases of recipe orientations
		// This way the player can arrange the ingredients in the item bowls as he likes
		// without resorting to specifying a fixed orientation
		for(int orientation = 0; orientation < 4; orientation++) {
			int offset = orientation * 2;
			recipeTestInventory.setStack(2, enchanterBlockEntity.virtualInventoryIncludingBowlStacks.getStack((offset + 8) % 8 + 2));
			recipeTestInventory.setStack(3, enchanterBlockEntity.virtualInventoryIncludingBowlStacks.getStack((offset + 1 + 8) % 8 + 2));
			recipeTestInventory.setStack(4, enchanterBlockEntity.virtualInventoryIncludingBowlStacks.getStack((offset + 2 + 8) % 8 + 2));
			recipeTestInventory.setStack(5, enchanterBlockEntity.virtualInventoryIncludingBowlStacks.getStack((offset + 3 + 8) % 8 + 2));
			recipeTestInventory.setStack(6, enchanterBlockEntity.virtualInventoryIncludingBowlStacks.getStack((offset + 4 + 8) % 8 + 2));
			recipeTestInventory.setStack(7, enchanterBlockEntity.virtualInventoryIncludingBowlStacks.getStack((offset + 5 + 8) % 8 + 2));
			recipeTestInventory.setStack(8, enchanterBlockEntity.virtualInventoryIncludingBowlStacks.getStack((offset + 6 + 8) % 8 + 2));
			recipeTestInventory.setStack(9, enchanterBlockEntity.virtualInventoryIncludingBowlStacks.getStack((offset + 7 + 8) % 8 + 2));
			
			EnchanterRecipe enchanterRecipe = world.getRecipeManager().getFirstMatch(SpectrumRecipeTypes.ENCHANTER, enchanterBlockEntity.virtualInventoryIncludingBowlStacks, world).orElse(null);
			if (enchanterRecipe == null) {
				EnchantmentUpgradeRecipe enchantmentUpgradeRecipe = world.getRecipeManager().getFirstMatch(SpectrumRecipeTypes.ENCHANTMENT_UPGRADE, enchanterBlockEntity.inventory, world).orElse(null);
				if (enchantmentUpgradeRecipe == null) {
					enchanterBlockEntity.currentRecipe = null;
				} else {
					enchanterBlockEntity.currentRecipe = enchantmentUpgradeRecipe;
					enchanterBlockEntity.virtualInventoryRecipeOrientation = orientation;
					enchanterBlockEntity.virtualInventoryIncludingBowlStacks = recipeTestInventory;
					enchanterBlockEntity.craftingTimeTotal = enchantmentUpgradeRecipe.getRequiredItemCount();
				}
			} else {
				enchanterBlockEntity.currentRecipe = enchanterRecipe;
				enchanterBlockEntity.virtualInventoryRecipeOrientation = orientation;
				enchanterBlockEntity.virtualInventoryIncludingBowlStacks = recipeTestInventory;
				enchanterBlockEntity.craftingTimeTotal = (int) Math.ceil(enchanterRecipe.getCraftingTime() / enchanterBlockEntity.upgrades.get(Upgradeable.UpgradeType.SPEED));
			}
			
			if (enchanterBlockEntity.currentRecipe != previousRecipe) {
				enchanterBlockEntity.updateInClientWorld();
				//SpectrumS2CPackets.sendCancelBlockBoundSoundInstance((ServerWorld) enchanterBlockEntity.world, enchanterBlockEntity.pos);
			}
		}
		
		return false;
	}
	
	public void inventoryChanged() {
		virtualInventoryIncludingBowlStacks = new SimpleInventory(INVENTORY_SIZE + 8);
		virtualInventoryIncludingBowlStacks.setStack(0, this.inventory.getStack(0)); // center item
		virtualInventoryIncludingBowlStacks.setStack(1, this.inventory.getStack(1)); // knowledge drop
		virtualInventoryIncludingBowlStacks.setStack(2, getItemBowlStack(this.world, pos.add(5, 0, -3)));
		virtualInventoryIncludingBowlStacks.setStack(3, getItemBowlStack(this.world, pos.add(5, 0, 3)));
		virtualInventoryIncludingBowlStacks.setStack(4, getItemBowlStack(this.world, pos.add(3, 0, 5)));
		virtualInventoryIncludingBowlStacks.setStack(5, getItemBowlStack(this.world, pos.add(-3, 0, 5)));
		virtualInventoryIncludingBowlStacks.setStack(6, getItemBowlStack(this.world, pos.add( -5, 0, 3)));
		virtualInventoryIncludingBowlStacks.setStack(7, getItemBowlStack(this.world, pos.add(-5, 0, -3)));
		virtualInventoryIncludingBowlStacks.setStack(8, getItemBowlStack(this.world, pos.add(-3, 0, -5)));
		virtualInventoryIncludingBowlStacks.setStack(9, getItemBowlStack(this.world, pos.add(3, 0, -5)));
		
		this.inventory.markDirty();
		this.virtualInventoryIncludingBowlStacks.markDirty();
		this.inventoryChanged = true;
	}
	
	public ItemStack getItemBowlStack(World world, BlockPos blockPos) {
		BlockEntity blockEntity = world.getBlockEntity(blockPos);
		if(blockEntity instanceof ItemBowlBlockEntity itemBowlBlockEntity) {
			return itemBowlBlockEntity.getInventory().getStack(0);
		} else {
			return ItemStack.EMPTY;
		}
	}
	
	public void playSound(SoundEvent soundEvent, float volume) {
		Random random = world.random;
		world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), soundEvent, SoundCategory.BLOCKS, volume, 0.9F + random.nextFloat() * 0.15F);
	}
	
	private static void grantPlayerEnchantingAdvancement(World world, UUID playerUUID, ItemStack resultStack, int levels) {
		ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity) PlayerOwned.getPlayerEntityIfOnline(world, playerUUID);
		if(serverPlayerEntity != null) {
			serverPlayerEntity.incrementStat(Stats.ENCHANT_ITEM);
			Criteria.ENCHANTED_ITEM.trigger(serverPlayerEntity, resultStack, levels);
		}
	}
	
	// PLAYER OWNED
	// "owned" is not to be taken literally here. The owner
	// is always set to the last player interacted with to trigger advancements
	@Override
	public UUID getOwnerUUID() {
		return this.ownerUUID;
	}
	
	@Override
	public void setOwner(PlayerEntity playerEntity) {
		this.ownerUUID = playerEntity.getUuid();
	}
	
	// UPGRADEABLE
	public void resetUpgrades() {
		this.upgrades = null;
	}
	
	public void calculateUpgrades() {
		Pair<Integer, Map<UpgradeType, Double>> upgrades = Upgradeable.checkUpgradeMods(world, pos, 2, 0);
		this.upgrades = upgrades.getRight();
	}
	
}
