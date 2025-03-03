package de.dafuqs.spectrum.enchantments;

import de.dafuqs.spectrum.SpectrumCommon;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;

/**
 * Increases the speed of shot arrows and makes them invisible
 */
public class SniperEnchantment extends SpectrumEnchantment {

	public SniperEnchantment(Rarity weight, EquipmentSlot... slotTypes) {
		super(weight, EnchantmentTarget.CROSSBOW, slotTypes);
	}

	public int getMinPower(int level) {
		return 20;
	}

	public int getMaxPower(int level) {
		return super.getMinPower(level) + 30;
	}

	public int getMaxLevel() {
		return 1;
	}

	public boolean canAccept(Enchantment other) {
		return other.equals(Enchantments.MULTISHOT) && super.canAccept(other);
	}

}

