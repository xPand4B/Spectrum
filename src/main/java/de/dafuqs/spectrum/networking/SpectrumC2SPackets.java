package de.dafuqs.spectrum.networking;

import de.dafuqs.spectrum.SpectrumCommon;
import de.dafuqs.spectrum.blocks.particle_spawner.ParticleSpawnerBlockEntity;
import de.dafuqs.spectrum.inventories.BedrockAnvilScreenHandler;
import de.dafuqs.spectrum.inventories.ParticleSpawnerScreenHandler;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.SharedConstants;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

public class SpectrumC2SPackets {

	public static final Identifier RENAME_ITEM_IN_BEDROCK_ANVIL_PACKET_ID = new Identifier(SpectrumCommon.MOD_ID, "rename_item_in_bedrock_anvil");
	public static final Identifier ADD_LORE_IN_BEDROCK_ANVIL_PACKET_ID = new Identifier(SpectrumCommon.MOD_ID, "add_lore_to_item_in_bedrock_anvil");
	public static final Identifier CHANGE_PARTICLE_SPAWNER_SETTINGS_PACKET_ID = new Identifier(SpectrumCommon.MOD_ID, "change_particle_spawner_settings");

	public static void registerC2SReceivers() {
		ServerPlayNetworking.registerGlobalReceiver(RENAME_ITEM_IN_BEDROCK_ANVIL_PACKET_ID, (server, player, handler, buf, responseSender) -> {
			String name = buf.readString();

			if (player.currentScreenHandler instanceof BedrockAnvilScreenHandler) {
				BedrockAnvilScreenHandler bedrockAnvilScreenHandler = (BedrockAnvilScreenHandler)player.currentScreenHandler;
				String string = SharedConstants.stripInvalidChars(name);
				if (string.length() <= 50) {
					bedrockAnvilScreenHandler.setNewItemName(string);
				}
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(ADD_LORE_IN_BEDROCK_ANVIL_PACKET_ID, (server, player, handler, buf, responseSender) -> {
			String lore = buf.readString();

			if (player.currentScreenHandler instanceof BedrockAnvilScreenHandler) {
				BedrockAnvilScreenHandler bedrockAnvilScreenHandler = (BedrockAnvilScreenHandler) player.currentScreenHandler;
				String string = SharedConstants.stripInvalidChars(lore);
				if (string.length() <= 256) {
					bedrockAnvilScreenHandler.setNewItemLore(string);
				}
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(CHANGE_PARTICLE_SPAWNER_SETTINGS_PACKET_ID, (server, player, handler, buf, responseSender) -> {
			// receive the client packet...
			if(player.currentScreenHandler instanceof ParticleSpawnerScreenHandler) {
				ParticleSpawnerScreenHandler particleSpawnerScreenHandler = (ParticleSpawnerScreenHandler) player.currentScreenHandler;
				ParticleSpawnerBlockEntity blockEntity = particleSpawnerScreenHandler.getBlockEntity();
				if(blockEntity != null) {
					/// ...apply the new settings...
					blockEntity.applySettings(buf);

					// ...and distribute it to all clients again
					PacketByteBuf packetByteBuf = PacketByteBufs.create();
					packetByteBuf.writeBlockPos(blockEntity.getPos());
					blockEntity.writeSettings(packetByteBuf);

					// Iterate over all players tracking a position in the world and send the packet to each player
					for (ServerPlayerEntity serverPlayerEntity : PlayerLookup.tracking((ServerWorld) blockEntity.getWorld(), blockEntity.getPos())) {
						ServerPlayNetworking.send(serverPlayerEntity, SpectrumS2CPackets.CHANGE_PARTICLE_SPAWNER_SETTINGS_CLIENT_PACKET_ID, packetByteBuf);
					}
				}
			}
		});
		
	}

}
