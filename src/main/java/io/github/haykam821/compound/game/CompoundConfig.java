package io.github.haykam821.compound.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.compound.game.board.BoardConfig;
import net.minecraft.SharedConstants;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;

public record CompoundConfig(
	BoardConfig boardConfig,
	SoundConfig soundConfig,
	Vec3d spectatorSpawnOffset,
	int ticksUntilClose
) {
	public static final Codec<CompoundConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			BoardConfig.CODEC.optionalFieldOf("board", BoardConfig.DEFAULT).forGetter(CompoundConfig::boardConfig),
			SoundConfig.CODEC.optionalFieldOf("sounds", SoundConfig.DEFAULT).forGetter(CompoundConfig::soundConfig),
			Vec3f.CODEC.xmap(Vec3d::new, Vec3f::new).optionalFieldOf("spectator_spawn_offset", new Vec3d(0, 2, 0)).forGetter(CompoundConfig::spectatorSpawnOffset),
			Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("ticks_until_close", SharedConstants.TICKS_PER_SECOND * 5).forGetter(CompoundConfig::ticksUntilClose)
		).apply(instance, CompoundConfig::new);
	});
}
