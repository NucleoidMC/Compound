package io.github.haykam821.compound.game.board;

import java.util.List;
import java.util.Optional;

import eu.pb4.mapcanvas.api.core.CombinedPlayerCanvas;
import eu.pb4.mapcanvas.api.core.DrawableCanvas;
import eu.pb4.mapcanvas.api.core.PlayerCanvas;
import eu.pb4.mapcanvas.api.utils.CanvasUtils;
import io.github.haykam821.compound.game.board.tile.TileCanvasMap;
import io.github.haykam821.compound.game.board.tile.TilePos;
import io.github.haykam821.compound.game.board.tile.TileReducer;
import net.minecraft.item.FilledMapItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

public class Board {
	private static final int RENDER_SCALE = FilledMapItem.field_30907;

	private final BoardConfig config;
	private final Random random;

	private final BoardGrid grid;
	private int score = 0;

	private final CombinedPlayerCanvas canvas;

	public Board(BoardConfig config, Random random) {
		this.config = config;
		this.random = random;

		// Initialize grid
		this.grid = new BoardGrid(config);
		this.spawnTiles(config.initialSpawnCount());

		// Initialize canvas
		int tilePadding = config.tilePadding();
		int startMultiplier = config.tileSize() + tilePadding;

		int width = this.grid.getWidth() * startMultiplier + tilePadding;
		int height = this.grid.getHeight() * startMultiplier + tilePadding;

		this.canvas = DrawableCanvas.create(width, height);
	}

	private void spawnTile() {
		Optional<Long> maybeValue = this.config.spawns().getDataOrEmpty(random);

		if (maybeValue.isPresent()) {
			this.grid.setRandomEmpty(maybeValue.get(), this.random);
		}
	}

	private void spawnTiles(int count) {
		for (int iteration = 0; iteration < count; iteration++) {
			this.spawnTile();
		}
	}

	public BoardChange slide(SlideDirection direction) {
		int oldGridHashCode = this.grid.hashCode();
		int oldScore = this.score;

		int iterations = direction.getIterations(grid);

		for (int iteration = 0; iteration < iterations; iteration++) {
			List<TilePos> positions = direction.getPositions(grid, iteration);
			this.score += this.grid.reduce(positions, TileReducer.MERGE);
		}

		if (this.grid.hashCode() != oldGridHashCode) {
			this.spawnTiles(this.config.slideSpawnCount());
			return this.score > oldScore ? BoardChange.MERGE : BoardChange.SLIDE;
		}

		return BoardChange.NONE;
	}

	public boolean isMovePossible() {
		return this.grid.hasEmptyPosition() || this.grid.hasMatch();
	}

	public void render() {
		CanvasUtils.clear(canvas, TileCanvasMap.GRID_BACKGROUND_COLOR);

		int width = this.grid.getWidth();
		int height = this.grid.getHeight();

		int tileSize = this.config.tileSize();
		int tilePadding = this.config.tilePadding();

		int startMultiplier = (tileSize + tilePadding);

		for (int tileX = 0; tileX < width; tileX++) {
			for (int tileY = 0; tileY < height; tileY++) {
				long value = this.grid.get(tileX, tileY);

				int startX = (tileX + 1) * startMultiplier;
				int startY = (tileY + 1) * startMultiplier;

				DrawableCanvas tileCanvas = TileCanvasMap.DEFAULT.get(value);

				int x = canvas.getWidth() - startX * RENDER_SCALE;
				int y = canvas.getHeight() - startY * RENDER_SCALE;
				int size = tileSize * RENDER_SCALE;

				if (tileCanvas == null) {
					CanvasUtils.fill(canvas, x, y, x + size, y + size, TileCanvasMap.EMPTY_BACKGROUND_COLOR);
				} else {
					CanvasUtils.draw(canvas, x, y, size, size, tileCanvas);
				}
			}
		}

		this.canvas.sendUpdates();
	}

	public BlockPos getDisplayPos() {
		int tilePadding = this.config.tilePadding();
		int startMultiplier = this.config.tileSize() + tilePadding;

		int x = this.grid.getWidth() * startMultiplier - tilePadding;
		int y = this.grid.getHeight() * startMultiplier - tilePadding;

		return new BlockPos(x, y, 0);
	}

	public Vec3d getSpawnPos() {
		BlockPos displayPos = this.getDisplayPos();
		return new Vec3d(displayPos.getX() / 2d, displayPos.getY() / 2d, displayPos.getY() * -1.2);
	}

	public int getSpawnAngle() {
		return 0;
	}

	public int getScore() {
		return this.score;
	}

	public PlayerCanvas getCanvas() {
		return this.canvas;
	}
}
