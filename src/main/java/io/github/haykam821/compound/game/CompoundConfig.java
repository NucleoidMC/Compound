package io.github.haykam821.compound.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.compound.game.board.BoardConfig;
import net.minecraft.SharedConstants;
import net.minecraft.world.phys.Vec3;

public record CompoundConfig(
	BoardConfig boardConfig,
	SoundConfig soundConfig,
	Vec3 spectatorSpawnOffset,
	int ticksUntilClose
) {
	public static final MapCodec<CompoundConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> {
		return instance.group(
			BoardConfig.CODEC.optionalFieldOf("board", BoardConfig.DEFAULT).forGetter(CompoundConfig::boardConfig),
			SoundConfig.CODEC.optionalFieldOf("sounds", SoundConfig.DEFAULT).forGetter(CompoundConfig::soundConfig),
			Vec3.CODEC.optionalFieldOf("spectator_spawn_offset", new Vec3(0, 2, 0)).forGetter(CompoundConfig::spectatorSpawnOffset),
			Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("ticks_until_close", SharedConstants.TICKS_PER_SECOND * 5).forGetter(CompoundConfig::ticksUntilClose)
		).apply(instance, CompoundConfig::new);
	});
}
