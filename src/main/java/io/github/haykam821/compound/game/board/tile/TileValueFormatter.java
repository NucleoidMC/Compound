package io.github.haykam821.compound.game.board.tile;

import java.text.NumberFormat;
import java.util.Locale;

public class TileValueFormatter {
	private static final NumberFormat FORMATTER = NumberFormat.getInstance(Locale.ROOT);

	public static String format(long value) {
		return FORMATTER.format(value);
	}
}
