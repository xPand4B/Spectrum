package de.dafuqs.spectrum.items.tools;

import de.dafuqs.spectrum.interfaces.PreEnchantedTooltip;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.List;

public class BedrockShovelItem extends ShovelItem implements PreEnchantedTooltip {

	public BedrockShovelItem(ToolMaterial material, int attackDamage, float attackSpeed, Settings settings) {
		super(material, attackDamage, attackSpeed, settings);
	}

	@Override
	public boolean isDamageable() {
		return false;
	}

	@Override
	public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
		super.appendTooltip(itemStack, world, tooltip, tooltipContext);
		addPreEnchantedTooltip(tooltip, itemStack);
	}

}