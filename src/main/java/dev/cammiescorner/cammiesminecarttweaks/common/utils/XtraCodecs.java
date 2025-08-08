package dev.cammiescorner.cammiesminecarttweaks.common.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.util.UUID;

public class XtraCodecs {
	public static final Codec<UUID> UUID_CODEC = RecordCodecBuilder.create(uuidInstance -> uuidInstance.group(
		Codec.LONG.fieldOf("most_sig_bits").forGetter(UUID::getMostSignificantBits),
		Codec.LONG.fieldOf("least_sig_bits").forGetter(UUID::getLeastSignificantBits)
	).apply(uuidInstance, UUID::new));

	public static final PacketCodec<PacketByteBuf, UUID> UUID_PACKET_CODEC = PacketCodec.tuple(
		PacketCodecs.VAR_LONG,
		UUID::getMostSignificantBits,

		PacketCodecs.VAR_LONG,
		UUID::getLeastSignificantBits,

		UUID::new
	);
}
