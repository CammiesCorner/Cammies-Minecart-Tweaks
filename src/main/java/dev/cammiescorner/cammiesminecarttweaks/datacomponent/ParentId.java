package dev.cammiescorner.cammiesminecarttweaks.datacomponent;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public record ParentId(UUID value) {
	public static final Codec<ParentId> CODEC = UUIDUtil.CODEC.xmap(ParentId::new, ParentId::value);
	public static final StreamCodec<ByteBuf, ParentId> STREAM_CODEC = UUIDUtil.STREAM_CODEC.map(ParentId::new, ParentId::value);
}
