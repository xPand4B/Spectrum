package de.dafuqs.spectrum.registries;

import de.dafuqs.spectrum.SpectrumCommon;
import de.dafuqs.spectrum.enums.PedestalRecipeTier;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import vazkii.patchouli.api.IMultiblock;
import vazkii.patchouli.api.PatchouliAPI;
import vazkii.patchouli.common.multiblock.StateMatcher;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpectrumMultiblocks {

	public static final Map<Identifier, IMultiblock> MULTIBLOCKS = new ConcurrentHashMap<>();

	public static Identifier PEDESTAL_SIMPLE_STRUCTURE_IDENTIFIER;
	public static Identifier PEDESTAL_ADVANCED_STRUCTURE_IDENTIFIER_CHECK;
	public static Identifier PEDESTAL_ADVANCED_STRUCTURE_IDENTIFIER_DISPLAY;
	public static Identifier PEDESTAL_COMPLEX_STRUCTURE_IDENTIFIER_CHECK;
	public static Identifier PEDESTAL_COMPLEX_STRUCTURE_IDENTIFIER_DISPLAY;

	public static Identifier FUSION_SHRINE_IDENTIFIER;
	public static Identifier ENCHANTER_IDENTIFIER;


	private static Identifier registerMultiBlock(String identifierString, String[][] structure, Object[] targetBlocks) {
		Identifier identifier = new Identifier(SpectrumCommon.MOD_ID, identifierString);
		IMultiblock multiblock = PatchouliAPI.get().makeMultiblock(structure, targetBlocks);
		MULTIBLOCKS.put(identifier, PatchouliAPI.get().registerMultiblock(identifier, multiblock));
		return identifier;
	}

	public static void register() {
		registerPedestal();
		registerFusionShrine();
		registerEnchanter();
	}

	private static void registerFusionShrine() {
		Object[] targetBlocks = {
				'X', "#spectrum:polished_base_blocks",
				'P', "#spectrum:pillar_base_blocks",
				'L', "#spectrum:gemstone_lamps",
				'L', "#spectrum:polished_base_block_slabs",
				'S', "#spectrum:gemstone_storage_blocks",
				'C', "#spectrum:chiseled_base_blocks",
				'F', "#spectrum:fusion_shrines",
				'_', StateMatcher.ANY,
				'0', "#spectrum:polished_base_blocks",
		};

		String[][] structure = {
				{ "_________", "_L_____L_", "_________", "_________", "_________", "_________", "_________", "_L_ ___L_", "_________" },
				{ "_________", "_S_____S_", "_________", "_________", "_________", "_________", "_________", "_S_____S_", "_________" },
				{ "_________", "_P_____P_", "_________", "_________", "_________", "_________", "_________", "_P_____P_", "_________" },
				{ "__XXXXX__", "_P_____P_", "X_______X", "X_______X", "X___F___X", "X_______X", "X_______X", "_P_____P_", "__XXXXX__" },
				{ "__XXXXX__", "_XXXXXXX_", "XXCXXXCXX", "XXXLLLXXX", "XXXL0LXXX", "XXXLLLXXX", "XXCXXXCXX", "_XXXXXXX_", "__XXXXX__" }
		};
		FUSION_SHRINE_IDENTIFIER = registerMultiBlock("fusion_shrine_structure", structure, targetBlocks);
	}

	private static void registerEnchanter() {
		Object[] targetBlocks = {
				'X', "#spectrum:polished_base_blocks",
				'P', "#spectrum:pillar_base_blocks",
				'B', "#spectrum:polished_base_block_slabs",
				'L', "#spectrum:gemstone_lamps",
				'G', "#spectrum:gemstone_storage_blocks",
				'F', "spectrum:enchanter",
				'C', "spectrum:liquid_crystal",
				'I', "#spectrum:item_bowls",
				'_', StateMatcher.ANY,
				'0', "#spectrum:polished_base_blocks",
		};

		String[][] structure = {
				{ "__I_____I__", "___________", "I_________I", "___________", "___________", "_____F_____", "___________", "___________", "I_________I", "___________", "__I_____I__",  },
				{ "__L_____L__", "___________", "L_G_____G_L", "____BBB____", "___BXXXB___", "___BXXXB___", "___BXXXB___", "____BBB____", "L_G_____G_L", "___________", "__L_____L__",  },
				{ "__P_____P__", "____BBB____", "P_P_XXX_P_P", "____XXX____", "_BXXXXXXXB_", "_BXXXXXXXB_", "_BXXXXXXXB_", "____XXX____", "P_P_XXX_P_P", "____BBB____", "__P_____P__",  },
				{ "_XXXXXXXXX_", "XCCCXXXCCCX", "XCXCXXXCXCX", "XCCCXXXCCCX", "XXXXXXXXXXX", "XXXXX0XXXXX", "XXXXXXXXXXX", "XCCCXXXCCCX", "XCXCXXXCXCX", "XCCCXXXCCCX", "_XXXXXXXXX_",  },
				{ "___________", "_XXX___XXX_", "_X_X___X_X_", "_XXX___XXX_", "___________", "___________", "___________", "_XXX___XXX_", "_X_X___X_X_", "_XXX___XXX_", "___________"   }
		};
		ENCHANTER_IDENTIFIER = registerMultiBlock("enchanter_structure", structure, targetBlocks);
	}

	private static void registerPedestal() {
		// since the structure is using rotation on blocks from a block tag
		// (that's impossible to define via string) we have to split targets for
		// check (if structure is built) and display (for auto building via debug item and manual)
		Object[] targetBlocksCheck = {
				'X', "#spectrum:polished_base_blocks",
				'P', "#spectrum:pillar_base_blocks",
				'p', "#spectrum:pillar_base_blocks",
				'Q', "#spectrum:pillar_base_blocks",
				'L', "#spectrum:gemstone_lamps",
				'S', "#spectrum:gemstone_storage_blocks",
				'C', "#spectrum:chiseled_base_blocks",
				'R', "#spectrum:basic_gemstone_chiseled_base_blocks",
				'O', "#spectrum:onyx_chiseled_base_blocks",
				'm', "#spectrum:moonstone_chiseled_base_blocks",
				'M', "#spectrum:moonstone_chiseled_base_blocks",
				'n', "#spectrum:polished_base_blocks_or_moonstone_chiseled",
				'N', "#spectrum:polished_base_blocks_or_moonstone_chiseled",
				'A', "#spectrum:pedestals",
				'a', "#spectrum:pedestals",
				'B', "#spectrum:pedestals",
				'_', StateMatcher.ANY,
				'0', StateMatcher.ANY
		};

		Object[] targetBlocksDisplay = {
				'X', "spectrum:polished_basalt",
				'P', "spectrum:polished_basalt_pillar[axis=x]",
				'p', "spectrum:polished_basalt_pillar[axis=z]",
				'Q', "spectrum:polished_basalt_pillar[axis=y]",
				'L', "spectrum:amethyst_basalt_lamp",
				'S', "spectrum:amethyst_storage_block",
				'C', "spectrum:chiseled_polished_basalt",
				'R', "spectrum:amethyst_chiseled_basalt",
				'O', "spectrum:onyx_chiseled_basalt",
				'm', "spectrum:moonstone_chiseled_basalt[axis=x]",
				'M', "spectrum:moonstone_chiseled_basalt[axis=y]",
				'n', "spectrum:polished_basalt",
				'N', "spectrum:polished_basalt",
				'A', "spectrum:pedestal_all_basic",
				'a', "spectrum:pedestal_onyx",
				'B', "spectrum:pedestal_moonstone",
				'_', StateMatcher.ANY,
				'0', StateMatcher.ANY
		};

		String[][] tier1Structure = {
				{ "C_________C", "___________", "___________", "___________", "___________", "___________", "___________", "___________", "___________", "___________", "C_________C" },
				{ "Q_________Q", "___________", "__S_____S__", "___________", "___________", "___________", "___________", "___________", "__S_____S__", "___________", "Q_________Q" },
				{ "X_________X", "___________", "__Q_____Q__", "___________", "___________", "_____A_____", "___________", "___________", "__Q_____Q__", "___________", "X_________X" },
				{ "___________", "___XXXXX___", "__XXXXXXX__", "_XXXXXXXXX_", "_XXXXXXXXX_", "_XXXX0XXXX_", "_XXXXXXXXX_", "_XXXXXXXXX_", "__XXXXXXX__", "___XXXXX___", "___________" }
		};
		PEDESTAL_SIMPLE_STRUCTURE_IDENTIFIER = registerMultiBlock("pedestal_simple_structure", tier1Structure, targetBlocksCheck);

		String[][] tier2Structure = {
				{ "_____________", "_SppR___RppS_", "_P_________P_", "_P_________P_", "_R_________R_", "_____________", "_____________", "_____________", "_R_________R_", "_P_________P_", "_P_________P_", "_SppR___RppS_", "_____________" },
				{ "_____________", "_Q__Q___Q__Q_", "_____________", "_____________", "_Q_________Q_", "_____________", "_____________", "_____________", "_Q_________Q_", "_____________", "_____________", "_Q__Q___Q__Q_", "_____________" },
				{ "_____________", "_C__Q___Q__C_", "_____________", "_____________", "_Q_________Q_", "_____________", "_____________", "_____________", "_Q_________Q_", "_____________", "_____________", "_C__Q___Q__C_", "_____________" },
				{ "_____________", "_Q__L___L__Q_", "_____________", "___S_____S___", "_L_________L_", "_____________", "_____________", "_____________", "_L_________L_", "___S_____S___", "_____________", "_Q__L___L__Q_", "_____________" },
				{ "_____________", "_X__Q___Q__X_", "_____________", "___Q_____Q___", "_Q_________Q_", "_____________", "______a______", "_____________", "_Q_________Q_", "___Q_____Q___", "_____________", "_X__Q___Q__X_", "_____________" },
				{ "XXXXXXXXXXXXX", "XXnnXnnnXnnXX", "XNXXXXXXXXXNX", "XNXXXXXXXXXNX", "XXXXXRXRXXXXX", "XNXXRXXXRXXNX", "XNXXXX0XXXXNX", "XNXXRXXXRXXNX", "XXXXXRXRXXXXX", "XNXXXXXXXXXNX", "XNXXXXXXXXXNX", "XXnnXnnnXnnXX", "XXXXXXXXXXXXX" }
		};
		PEDESTAL_ADVANCED_STRUCTURE_IDENTIFIER_CHECK = registerMultiBlock("pedestal_advanced_structure_check", tier2Structure, targetBlocksCheck);
		PEDESTAL_ADVANCED_STRUCTURE_IDENTIFIER_DISPLAY = registerMultiBlock("pedestal_advanced_structure_display", tier2Structure, targetBlocksDisplay);

		String[][] tier3Structure = {
				{ "_____________", "____XpSpX____", "_____________", "_____________", "_X__OpppO__X_", "_P__P___P__P_", "_S__P___P__S_", "_P__P___P__P_", "_X__OpppO__X_", "_____________", "_____________", "____XpSpX____", "_____________" },
				{ "_____________", "_SppR___RppS_", "_P__P___P__P_", "_P__P___P__P_", "_RppX___XppR_", "_____________", "_____________", "_____________", "_RppX___XppR_", "_P__P___P__P_", "_P__P___P__P_", "_SppR___RppS_", "_____________" },
				{ "_____________", "_Q__Q___Q__Q_", "_____________", "_____________", "_Q_________Q_", "_____________", "_____________", "_____________", "_Q_________Q_", "_____________", "_____________", "_Q__Q___Q__Q_", "_____________" },
				{ "_____________", "_C__Q___Q__C_", "_____________", "_____________", "_Q_________Q_", "_____________", "_____________", "_____________", "_Q_________Q_", "_____________", "_____________", "_C__Q___Q__C_", "_____________" },
				{ "_____________", "_Q__L___L__Q_", "_____________", "___S_____S___", "_L_________L_", "_____________", "_____________", "_____________", "_L_________L_", "___S_____S___", "_____________", "_Q__L___L__Q_", "_____________" },
				{ "_____________", "_X__Q___Q__X_", "_____________", "___Q_____Q___", "_Q_________Q_", "_____________", "______B______", "_____________", "_Q_________Q_", "___Q_____Q___", "_____________", "_X__Q___Q__X_", "_____________" },
				{ "XXXXXXXXXXXXX", "XXmmXmmmXmmXX", "XMXXXXXXXXXMX", "XMXXXXXXXXXMX", "XXXXXRXRXXXXX", "XMXXRXXXRXXMX", "XMXXXX0XXXXMX", "XMXXRXXXRXXMX", "XXXXXRXRXXXXX", "XMXXXXXXXXXMX", "XMXXXXXXXXXMX", "XXmmXmmmXmmXX", "XXXXXXXXXXXXX" }
		};
		PEDESTAL_COMPLEX_STRUCTURE_IDENTIFIER_CHECK = registerMultiBlock("pedestal_complex_structure_check", tier3Structure, targetBlocksCheck);
		PEDESTAL_COMPLEX_STRUCTURE_IDENTIFIER_DISPLAY = registerMultiBlock("pedestal_complex_structure_display", tier3Structure, targetBlocksDisplay);
	}

	public static Identifier getDisplayStructureIdentifierForTier(@NotNull PedestalRecipeTier pedestalRecipeTier) {
		switch (pedestalRecipeTier) {
			case COMPLEX -> {
				return SpectrumMultiblocks.PEDESTAL_COMPLEX_STRUCTURE_IDENTIFIER_DISPLAY;
			}
			case ADVANCED -> {
				return SpectrumMultiblocks.PEDESTAL_ADVANCED_STRUCTURE_IDENTIFIER_DISPLAY;
			}
			case SIMPLE -> {
				return SpectrumMultiblocks.PEDESTAL_SIMPLE_STRUCTURE_IDENTIFIER;
			}
		}
		return null;
	}

	public static TranslatableText getPedestalStructureText(@NotNull PedestalRecipeTier pedestalRecipeTier) {
		switch (pedestalRecipeTier) {
			case COMPLEX -> {
				return new TranslatableText("multiblock.spectrum.pedestal.complex_structure");
			}
			case ADVANCED -> {
				return new TranslatableText("multiblock.spectrum.pedestal.advanced_structure");
			}
			case SIMPLE -> {
				return new TranslatableText("multiblock.spectrum.pedestal.simple_structure");
			}
		}
		return null;
	}

}