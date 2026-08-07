package dev.cammiescorner.cammiesminecarttweaks.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.UUID;

public class XtraCodecs {
	/**
	 * @deprecated Only kept for save file compatibility, will be replaced by {@link net.minecraft.core.UUIDUtil#CODEC}
	 */
	@Deprecated(forRemoval = true)
	public static final Codec<UUID> UUID_CODEC = RecordCodecBuilder.create(uuidInstance -> uuidInstance.group(
		Codec.LONG.fieldOf("most_sig_bits").forGetter(UUID::getMostSignificantBits),
		Codec.LONG.fieldOf("least_sig_bits").forGetter(UUID::getLeastSignificantBits)
	).apply(uuidInstance, UUID::new));
}
