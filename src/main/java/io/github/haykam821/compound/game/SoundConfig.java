package io.github.haykam821.compound.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public record SoundConfig(
	float volume,
	float pitch,
	SoundEvent slide,
	SoundEvent merge,
	SoundEvent gameEnd
) {
	public static final Codec<SoundConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			Codec.FLOAT.fieldOf("volume").forGetter(SoundConfig::volume),
			Codec.FLOAT.fieldOf("pitch").forGetter(SoundConfig::pitch),
			SoundEvent.DIRECT_CODEC.fieldOf("slide").forGetter(SoundConfig::slide),
			SoundEvent.DIRECT_CODEC.fieldOf("merge").forGetter(SoundConfig::merge),
			SoundEvent.DIRECT_CODEC.fieldOf("game_end").forGetter(SoundConfig::gameEnd)
		).apply(instance, SoundConfig::new);
	});

	public static final SoundConfig DEFAULT = new SoundConfig(0.5f, 1f, SoundEvents.UI_TOAST_OUT, SoundEvents.COMPOSTER_FILL_SUCCESS, SoundEvents.CREEPER_DEATH);
}
