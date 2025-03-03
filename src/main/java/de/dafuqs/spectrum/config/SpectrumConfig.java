package de.dafuqs.spectrum.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

import java.util.Arrays;
import java.util.List;

@Config(name = "Spectrum")
public class SpectrumConfig implements ConfigData {
	
	@Comment("""
The heights where citrine and topaz geodes will spawn
By default citrine will generate slightly below sea level (y=35-55)
while topaz will generate at the top of mountains (everywhere from y=70+)
if the worldgen has lots of high mountains consider raising the TopazGeodeMinFixedGenerationHeight""")
	public int CitrineGeodeMinAboveBottomGenerationHeight = 35;
	public int CitrineGeodeFixedMaxGenerationHeight = 60;
	public int TopazGeodeMinFixedGenerationHeight = 90;
	public int TopazGeodeMaxBelowTopGenerationHeight = 0;
	
	@Comment("Every x chunks there is a chance for a geode to generate")
	public int TopazGeodeChunkChance = 12;
	public int CitrineGeodeChunkChance = 40;
	public int MoonstoneGeodeChunkChance = 35;
	
	@Comment("The amount of colored tree patches to generate every X chunks")
	public int ColoredTreePatchChanceChunk = 50;

	@Comment("""
The chance that an Enderman is holding a special treasure block on spawn
Separate value for Endermen spawning in the end, since there are LOTS of them there
Those blocks do not gate progression, so it is not that drastic not finding any right away.
Better to let players stumble about them organically instead of forcing it.""")
	public float EndermanHoldingEnderTreasureChance = 0.08F;
	public float EndermanHoldingEnderTreasureInEndChance = 0.005F;

	@Comment("Worlds where shooting stars spawn for players. Shooting Stars will only spawn for players with sufficient progress in the mod")
	public List<String> ShootingStarWorlds = Arrays.asList("minecraft:overworld", "starry_sky:starry_sky");

	@Comment("Worlds where lightning strikes can spawn lightning stones")
	public List<String> LightningStonesWorlds = Arrays.asList("minecraft:overworld", "starry_sky:starry_sky");
	// chance for a lightning strike to spawn a lightning stone
	public float LightningStonesChance = 0.4F;

	@Comment("""
Shooting star spawns are checked every night between time 13000 and 22000, every 100 ticks (so 90 chances per night).
By default, there is a 0.02 ^= 2 % chance at each of those check times. Making it ~3 shooting star spawns per night.""")
	public float ShootingStarChance = 0.02F;

	@Comment("The biomes where the biome specific plants are growing")
	public List<String> MermaidsBrushGenerationBiomes = Arrays.asList("minecraft:ocean", "minecraft:cold_ocean", "minecraft_frozen_ocean", "minecraft:lukewarm_ocean", "minecraft:warm_ocean" ,"minecraft:deep_ocean", "minecraft:deep_cold_ocean", "minecraft:deep_frozen_ocean", "minecraft:deep_warm_ocean", "minecraft:deep_lukewarm_ocean");
	public List<String> QuitoxicReedsGenerationBiomes = Arrays.asList("minecraft:swamp");

	@Comment("""
How fast decay will be spreading on random tick
can be used to slow down propagation speed of decay in the worlds
decay does use very few resources, but if your fear of someone letting decay
spread free or using high random tick rates you can limit the rate here
1.0: every random tick (default)
0.5: Every second random tick
0.0: never (forbidden; players would be unable to progress)""")
	public float FadingDecayTickRate = 1.0F;
	public float FailingDecayTickRate = 1.0F;
	public float RuinDecayTickRate = 1.0F;

	@Comment("Enable or disable specific enchantments. Disabling the voiding enchantment will also disable the Oblivion Pickaxe")
	public boolean AutoSmeltEnchantmentEnabled = true;
	public boolean ExuberanceEnchantmentEnabled = true;
	public boolean InventoryInsertionEnchantmentEnabled = true;
	public boolean PestControlEnchantmentEnabled = true;
	public boolean ResonanceEnchantmentEnabled = true;
	public boolean VoidingEnchantmentEnabled = true;
	public boolean TreasureHunterEnchantmentEnabled = true;
	public boolean DisarmingEnchantmentEnabled = true;
	public boolean FirstStrikeEnchantmentEnabled = true;
	public boolean ImprovedCriticalEnchantmentEnabled = true;
	public boolean InertiaEnchantmentEnabled = true;
	public boolean CloversFavorEnchantmentEnabled = true;
	public boolean SniperEnchantmentEnabled = true;
	public boolean TightGripEnchantmentEnabled = true;
	public boolean DamageProofEnchantmentEnabled = true;
	
	@Comment("The max levels for all Enchantments")
	public int TreasureHunterMaxLevel = 3;
	public int DisarmingMaxLevel = 2;
	public int FirstStrikeMaxLevel = 2;
	public int ImprovedCriticalMaxLevel = 2;
	public int InertiaMaxLevel = 3;
	public int CloversFavorMaxLevel = 3;
	public int TightGripMaxLevel = 2;

	@Comment("Exuberance increases experience gained when killing mobs. With 25% bonus XP and 5 levels this would mean 2,25x XP on max level")
	public int ExuberanceMaxLevel = 5;
	public float ExuberanceBonusExperiencePercentPerLevel = 0.25F;

	@Comment("In vanilla, crits are a flat 50 % damage bonus. Improved Critical increases this damage by additional 50 % per level by default")
	public float ImprovedCriticalExtraDamageMultiplierPerLevel = 0.5F;

	@Comment("Flat additional damage dealt with each level of the First Strike enchantment")
	public float FirstStrikeDamagePerLevel = 2.0F;

	@Comment("The duration a glow ink sac gives night vision when wearing a glow vision helmet in seconds")
	public int GlowVisionGogglesDuration = 240;

	@Override
	public void validatePostLoad() throws ValidationException {
		if(FadingDecayTickRate <= 0) { FadingDecayTickRate = 1.0F; }
		if(FailingDecayTickRate <= 0) { FadingDecayTickRate = 1.0F; }
		if(RuinDecayTickRate <= 0) { RuinDecayTickRate = 1.0F; }
		if(ShootingStarChance <= 0) { ShootingStarChance = 0.01F; }
		if(LightningStonesChance <= 0) { ShootingStarChance = 0.3F; }
		if(EndermanHoldingEnderTreasureChance <= 0) { EndermanHoldingEnderTreasureChance = 0.05F; }
		if(TreasureHunterMaxLevel <= 0) { TreasureHunterMaxLevel = 3; }
		if(ExuberanceMaxLevel <= 0) { ExuberanceMaxLevel = 5; }
		if(ExuberanceBonusExperiencePercentPerLevel <= 0) { ExuberanceBonusExperiencePercentPerLevel = 0.2F; }
		if(ImprovedCriticalExtraDamageMultiplierPerLevel <= 0) { ImprovedCriticalExtraDamageMultiplierPerLevel = 0.5F; }
		if(FirstStrikeDamagePerLevel <= 0) { FirstStrikeDamagePerLevel = 3.0F; }
	}


}