package io.github.haykam821.compound;

import io.github.haykam821.compound.game.CompoundConfig;
import io.github.haykam821.compound.game.CompoundGame;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.api.game.GameType;

public class Compound implements ModInitializer {
	private static final String MOD_ID = "compound";

	private static final Identifier COMPOUND_ID = Compound.identifier("compound");
	public static final GameType<CompoundConfig> COMPOUND = GameType.register(COMPOUND_ID, CompoundConfig.CODEC, CompoundGame::open);

	@Override
	public void onInitialize() {
		return;
	}

	public static Identifier identifier(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
