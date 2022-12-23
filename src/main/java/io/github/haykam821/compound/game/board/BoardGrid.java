package io.github.haykam821.compound.game.board;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.haykam821.compound.game.board.tile.TilePos;
import io.github.haykam821.compound.game.board.tile.TileReducer;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.util.Util;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

public class BoardGrid {
	private final int width;
	private final int height;

	private final long[][] grid;

	public BoardGrid(BoardConfig config) {
		this.width = config.width();
		this.height = config.height();

		this.grid = new long[this.width][this.height];
	}

	public long get(int x, int y) {
		return this.grid[x][y];
	}

	public long get(TilePos pos) {
		return this.get(pos.x(), pos.y());
	}

	public long getBounded(int x, int y) {
		if (x < 0 || x >= this.width) return 0;
		if (y < 0 || y >= this.height) return 0;

		return this.get(x, y);
	}

	public long getBounded(TilePos pos) {
		return this.getBounded(pos.x(), pos.y());
	}

	public void set(int x, int y, long value) {
		this.grid[x][y] = value;
	}

	public void set(TilePos pos, long value) {
		this.set(pos.x(), pos.y(), value);
	}

	public boolean hasEmptyPosition() {
		for (int x = 0; x < this.width; x++) {
			for (int y = 0; y < this.height; y++) {
				if (this.get(x, y) <= 0) {
					return true;
				}
			}
		}

		return false;
	}

	private List<TilePos> getEmptyPositions() {
		List<TilePos> positions = new ArrayList<>(this.width * this.height);

		for (int x = 0; x < this.width; x++) {
			for (int y = 0; y < this.height; y++) {
				TilePos pos = new TilePos(x, y);

				if (this.get(pos) <= 0) {
					positions.add(pos);
				}
			}
		}

		return positions;
	}

	public boolean setRandomEmpty(long value, Random random) {
		List<TilePos> positions = this.getEmptyPositions();

		if (positions.isEmpty()) {
			return false;
		}

		TilePos pos = Util.getRandom(positions, random);
		this.set(pos, value);

		return true;
	}

	public List<TilePos> getRowPositions(int row) {
		List<TilePos> positions = new ArrayList<>(this.height);

		for (int x = 0; x < this.width; x++) {
			positions.add(new TilePos(x, row));
		}

		return positions;
	}

	public List<TilePos> getColumnPositions(int column) {
		List<TilePos> positions = new ArrayList<>(this.width);

		for (int y = 0; y < this.height; y++) {
			positions.add(new TilePos(column, y));
		}

		return positions;
	}

	public int reduce(List<TilePos> positions, TileReducer reducer) {
		LongList newValues = new LongArrayList();
		int score = 0;

		// Reduce values into new values list
		for (TilePos pos : positions) {
			score += reducer.reduce(this, this.get(pos), newValues);
		}

		// Apply new values to grid
		int index = 0;
		for (TilePos pos : positions) {
			long value = index >= newValues.size() ? 0 : newValues.getLong(index);
			this.set(pos, value);

			index += 1;
		}

		return score;
	}

	public boolean hasMatch() {
		for (int x = 0; x < this.width; x++) {
			for (int y = 0; y < this.height; y++) {
				long value = this.get(x, y);

				if (value > 0) {
					for (Direction direction : Direction.Type.HORIZONTAL) {
						long other = this.getBounded(x + direction.getOffsetX(), y + direction.getOffsetZ());

						if (value == other) {
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}

	@Override
	public String toString() {
		return "BoardGrid(" + this.width + ", " + this.height + ") " + Arrays.deepToString(this.grid);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof BoardGrid) {
			BoardGrid otherGrid = (BoardGrid) other;

			if (this.width == otherGrid.width && this.height == otherGrid.height) {
				return Arrays.deepEquals(this.grid, otherGrid.grid);
			}
		}

		return false;
	}

	@Override
	public int hashCode() {
		return Arrays.deepHashCode(this.grid);
	}
}
