package io.github.haykam821.compound.game.board.tile;

import eu.pb4.mapcanvas.api.core.CanvasColor;
import eu.pb4.mapcanvas.api.core.DrawableCanvas;
import eu.pb4.mapcanvas.api.font.CanvasFont;
import eu.pb4.mapcanvas.api.font.DefaultFonts;
import eu.pb4.mapcanvas.api.utils.CanvasUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public final class TileCanvasMap {
	public static final CanvasColor GRID_BACKGROUND_COLOR = CanvasUtils.findClosestColor(0xBBADA0);
	public static final CanvasColor EMPTY_BACKGROUND_COLOR = CanvasUtils.findClosestColor(0xCCC1B4);

	private static final int DARK_TEXT_COLOR = 0x776E65;
	private static final int LIGHT_TEXT_COLOR = 0xF9F6F2;

	private static final CanvasFont FONT = DefaultFonts.VANILLA;
	private static final int CANVAS_SIZE = 1;

	public static final TileCanvasMap DEFAULT = new TileCanvasMap()
		.put(2, 0xEEE4DA, DARK_TEXT_COLOR)
		.put(4, 0xEEE1C9, DARK_TEXT_COLOR)
		.put(8, 0xF3B27A, LIGHT_TEXT_COLOR)
		.put(16, 0xF69664, LIGHT_TEXT_COLOR)
		.put(32, 0xF77C5F, LIGHT_TEXT_COLOR)
		.put(64, 0xF75F3B, LIGHT_TEXT_COLOR)
		.put(128, 0xEDD073, LIGHT_TEXT_COLOR)
		.put(256, 0xEDCC62, LIGHT_TEXT_COLOR)
		.put(512, 0xEDC950, LIGHT_TEXT_COLOR)
		.put(1024, 0xEDC53F, LIGHT_TEXT_COLOR)
		.put(2048, 0xEDC22E, LIGHT_TEXT_COLOR);

	private final Long2ObjectMap<DrawableCanvas> map = new Long2ObjectOpenHashMap<>();

	private double getTextSize(String value, double maxWidth, double size) {
		while (size > 0) {
			int width = FONT.getTextWidth(value, size);

			if (width < maxWidth) {
				return size;
			} else {
				size -= 1;
			}
		}

		return 0;
	}

	private DrawableCanvas renderCanvas(String value, CanvasColor backgroundColor, CanvasColor textColor) {
		DrawableCanvas canvas = DrawableCanvas.create(CANVAS_SIZE, CANVAS_SIZE);
		CanvasUtils.clear(canvas, backgroundColor);

		double size = this.getTextSize(value, canvas.getWidth() * 0.8, canvas.getHeight() * 0.6);

		if (size > 0) {
			int width = FONT.getTextWidth(value, size);

			int x = (canvas.getWidth() - width) / 2;
			int y = (int) (canvas.getHeight() - size) / 2;

			FONT.drawText(canvas, value, x, y, size, textColor);
		}

		return canvas;
	}

	private DrawableCanvas renderCanvas(String value, int backgroundColor, int textColor) {
		return this.renderCanvas(value, CanvasUtils.findClosestColor(backgroundColor), CanvasUtils.findClosestColor(textColor));
	}

	private DrawableCanvas renderCanvas(long value, int backgroundColor, int textColor) {
		return this.renderCanvas(TileValueFormatter.format(value), backgroundColor, textColor);
	}

	private TileCanvasMap put(long value, int backgroundColor, int textColor) {
		this.map.put(value, this.renderCanvas(value, backgroundColor, textColor));
		return this;
	}

	public DrawableCanvas get(long value) {
		if (value <= 0) {
			return null;
		}

		return this.map.computeIfAbsent(value, v -> {
			return this.renderCanvas(value, 0x3C3A33, DARK_TEXT_COLOR);
		});
	}

	@Override
	public String toString() {
		return "TileCanvasMap{map=" + this.map + "}";
	}
}
