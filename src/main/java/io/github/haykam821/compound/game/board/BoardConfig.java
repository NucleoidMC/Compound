package io.github.haykam821.compound.game.board;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.collection.DataPool;

public record BoardConfig(
	int width,
	int height,
	int tileSize,
	int tilePadding,
	int initialSpawnCount,
	int slideSpawnCount,
	DataPool<Long> spawns
) {
	private static final int DEFAULT_SIZE = 4;
	private static final int DEFAULT_TILE_SIZE = 2;
	private static final int DEFAULT_TILE_PADDING = 1;

	private static final int DEFAULT_INITIAL_SPAWN_COUNT = 2;
	private static final int DEFAULT_SLIDE_SPAWN_COUNT = 1;
	private static final DataPool<Long> DEFAULT_SPAWNS = DataPool.<Long>builder()
		.add(2l, 9)
		.add(4l, 1)
		.build();

	public static final Codec<BoardConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			Codec.INT.optionalFieldOf("width", DEFAULT_SIZE).forGetter(BoardConfig::width),
			Codec.INT.optionalFieldOf("height", DEFAULT_SIZE).forGetter(BoardConfig::height),
			Codec.INT.optionalFieldOf("tile_size", DEFAULT_TILE_SIZE).forGetter(BoardConfig::tileSize),
			Codec.INT.optionalFieldOf("tile_padding", DEFAULT_TILE_PADDING).forGetter(BoardConfig::tilePadding),
			Codec.INT.optionalFieldOf("initial_spawn_count", DEFAULT_INITIAL_SPAWN_COUNT).forGetter(BoardConfig::initialSpawnCount),
			Codec.INT.optionalFieldOf("slide_spawn_count", DEFAULT_SLIDE_SPAWN_COUNT).forGetter(BoardConfig::slideSpawnCount),
			DataPool.createCodec(Codec.LONG).optionalFieldOf("spawns", DEFAULT_SPAWNS).forGetter(BoardConfig::spawns)
		).apply(instance, BoardConfig::new);
	});

	public static final BoardConfig DEFAULT = new BoardConfig(DEFAULT_SIZE, DEFAULT_SIZE, DEFAULT_TILE_SIZE, DEFAULT_TILE_PADDING, DEFAULT_INITIAL_SPAWN_COUNT, DEFAULT_SLIDE_SPAWN_COUNT, DEFAULT_SPAWNS);
}
