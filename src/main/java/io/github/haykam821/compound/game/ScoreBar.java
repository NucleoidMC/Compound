package io.github.haykam821.compound.game;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.common.widget.BossBarWidget;

public final class ScoreBar {
	private static final BossBar.Color COLOR = BossBar.Color.WHITE;
	private static final BossBar.Style STYLE = BossBar.Style.PROGRESS;

	private static final Text NAME = Text.translatable("gameType.compound.compound");
	private static final Formatting FORMATTING = Formatting.WHITE;

	private final CompoundGame game;
	private final BossBarWidget widget;

	public ScoreBar(CompoundGame game, GlobalWidgets widgets) {
		this.game = game;
		this.widget = widgets.addBossBar(this.getTitle(), COLOR, STYLE);
	}

	public void updateTitle() {
		this.widget.setTitle(this.getTitle());
	}

	private Text getTitle() {
		int score = this.game.getScore();
		return Text.translatable("text.compound.bar.title", NAME, score).formatted(FORMATTING);
	}
}
