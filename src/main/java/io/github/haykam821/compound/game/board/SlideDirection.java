package io.github.haykam821.compound.game.board;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.github.haykam821.compound.game.board.tile.TilePos;
import net.minecraft.server.network.ServerPlayerEntity;

public enum SlideDirection {
	UP(BoardGrid::getWidth, reversed(BoardGrid::getColumnPositions)),
	RIGHT(BoardGrid::getHeight, BoardGrid::getRowPositions),
	DOWN(BoardGrid::getWidth, BoardGrid::getColumnPositions),
	LEFT(BoardGrid::getHeight, reversed(BoardGrid::getRowPositions)),
	MULTIPLE(null, null);

	public static final SlideDirection[] DIRECTIONS = Arrays.stream(SlideDirection.values())
		.filter(SlideDirection::isDeterminate)
		.toArray(SlideDirection[]::new);

	private final Function<BoardGrid, Integer> iterationGetter;
	private final BiFunction<BoardGrid, Integer, List<TilePos>> positionGetter;

	private SlideDirection(Function<BoardGrid, Integer> iterationGetter, BiFunction<BoardGrid, Integer, List<TilePos>> positionGetter) {
		this.iterationGetter = iterationGetter;
		this.positionGetter = positionGetter;
	}

	public int getIterations(BoardGrid grid) {
		return this.iterationGetter.apply(grid);
	}

	public List<TilePos> getPositions(BoardGrid grid, int iteration) {
		return this.positionGetter.apply(grid, iteration);
	}

	public static SlideDirection fromMovement(ServerPlayerEntity player) {
		if (player.forwardSpeed != 0 && player.sidewaysSpeed != 0) {
			return SlideDirection.MULTIPLE;
		} else if (player.forwardSpeed > 0) {
			return SlideDirection.UP;
		} else if (player.sidewaysSpeed < 0) {
			return SlideDirection.RIGHT;
		} else if (player.forwardSpeed < 0) {
			return SlideDirection.DOWN;
		} else if (player.sidewaysSpeed > 0) {
			return SlideDirection.LEFT;
		} else {
			return null;
		}
	}

	public static boolean isDeterminate(SlideDirection direction) {
		return direction != null && direction != SlideDirection.MULTIPLE;
	}

	public static boolean isDifferent(SlideDirection current, SlideDirection last) {
		// Direction cannot change from multiple to a single direction
		if (last == SlideDirection.MULTIPLE) {
			return current == null;
		}

		return current != last;
	}

	private static BiFunction<BoardGrid, Integer, List<TilePos>> reversed(BiFunction<BoardGrid, Integer, List<TilePos>> positionGetter) {
		return (grid, iteration) -> {
			List<TilePos> positions = positionGetter.apply(grid, iteration);
			Collections.reverse(positions);
			return positions;
		};
	}
}
