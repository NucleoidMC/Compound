package io.github.haykam821.compound.game.board.tile;

import io.github.haykam821.compound.game.board.BoardGrid;
import it.unimi.dsi.fastutil.longs.LongList;

public interface TileReducer {
	public static TileReducer MERGE = (grid, value, newValues) -> {
		if (value > 0) {
			if (!newValues.isEmpty()) {
				int lastIndex = newValues.size() - 1;
				long lastValue = newValues.getLong(lastIndex);

				if (value == lastValue) {
					return newValues.set(newValues.size() - 1, value * 2);
				}
			}

			newValues.add(value);
		}

		return 0;
	};

	/**
	 * @param grid the grid the reducer is applying to
	 * @param value the current value being reduced
	 * @param newValues the list of new values to replace the row or column with
	 * @return the value to add to the score
	 */
	public long reduce(BoardGrid grid, long value, LongList newValues);
}
