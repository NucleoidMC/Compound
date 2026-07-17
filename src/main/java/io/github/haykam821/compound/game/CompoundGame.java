package io.github.haykam821.compound.game;

import eu.pb4.mapcanvas.api.utils.VirtualDisplay;
import io.github.haykam821.compound.game.board.Board;
import io.github.haykam821.compound.game.board.BoardChange;
import io.github.haykam821.compound.game.board.SlideDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.fantasy.util.VoidChunkGenerator;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.GameOpenContext;
import xyz.nucleoid.plasmid.api.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.player.JoinOfferResult;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class CompoundGame implements GamePlayerEvents.Add, GameActivityEvents.Destroy, GameActivityEvents.Tick, GamePlayerEvents.Remove, GamePlayerEvents.Accept, GamePlayerEvents.Offer, PlayerDamageEvent, PlayerDeathEvent {
	private final GameSpace gameSpace;
	private final ServerLevel world;
	private final CompoundConfig config;

	private final Board board;
	private final ScoreBar bar;
	private final VirtualDisplay display;

	private ServerPlayer mainPlayer;
	private SlideDirection lastSlideDirection;

	private int ticksUntilClose = -1;

	public CompoundGame(GameSpace gameSpace, ServerLevel world, CompoundConfig config, Board board, GlobalWidgets widgets, VirtualDisplay display) {
		this.gameSpace = gameSpace;
		this.world = world;
		this.config = config;

		this.board = board;
		this.bar = new ScoreBar(this, widgets);
		this.display = display;
	}

	public static void setRules(GameActivity activity) {
		activity.deny(GameRuleType.BLOCK_DROPS);
		activity.deny(GameRuleType.BREAK_BLOCKS);
		activity.deny(GameRuleType.CRAFTING);
		activity.deny(GameRuleType.DISMOUNT_VEHICLE);
		activity.deny(GameRuleType.FALL_DAMAGE);
		activity.deny(GameRuleType.FIRE_TICK);
		activity.deny(GameRuleType.FLUID_FLOW);
		activity.deny(GameRuleType.HUNGER);
		activity.deny(GameRuleType.ICE_MELT);
		activity.deny(GameRuleType.MODIFY_ARMOR);
		activity.deny(GameRuleType.MODIFY_INVENTORY);
		activity.deny(GameRuleType.PICKUP_ITEMS);
		activity.deny(GameRuleType.PLACE_BLOCKS);
		activity.deny(GameRuleType.PLAYER_PROJECTILE_KNOCKBACK);
		activity.deny(GameRuleType.PORTALS);
		activity.deny(GameRuleType.PVP);
		activity.deny(GameRuleType.SWAP_OFFHAND);
		activity.deny(GameRuleType.THROW_ITEMS);
		activity.deny(GameRuleType.TRIDENTS_LOYAL_IN_VOID);
		activity.deny(GameRuleType.UNSTABLE_TNT);
	}

	public static GameOpenProcedure open(GameOpenContext<CompoundConfig> context) {
		CompoundConfig config = context.config();
		Board board = new Board(config.boardConfig(), RandomSource.createThreadLocalInstance());

		RuntimeLevelConfig worldConfig = new RuntimeLevelConfig()
			.setGenerator(new VoidChunkGenerator(context.server().registryAccess().lookupOrThrow(Registries.BIOME)));

		return context.openWithLevel(worldConfig, (activity, world) -> {
			board.render();

			GlobalWidgets widgets = GlobalWidgets.addTo(activity);
			VirtualDisplay display = VirtualDisplay.builder(board.getCanvas(), board.getDisplayPos(), Direction.NORTH)
				.invisible()
				.build();

			CompoundGame phase = new CompoundGame(activity.getGameSpace(), world, config, board, widgets, display);
			CompoundGame.setRules(activity);

			// Listeners
			activity.listen(GamePlayerEvents.ADD, phase);
			activity.listen(GameActivityEvents.DESTROY, phase);
			activity.listen(GameActivityEvents.TICK, phase);
			activity.listen(GamePlayerEvents.ACCEPT, phase);
			activity.listen(GamePlayerEvents.OFFER, phase);
			activity.listen(PlayerDamageEvent.EVENT, phase);
			activity.listen(PlayerDeathEvent.EVENT, phase);
			activity.listen(GamePlayerEvents.REMOVE, phase);
		});
	}

	// Listeners
	@Override
	public void onAddPlayer(ServerPlayer player) {
		this.display.addPlayer(player);
		this.display.getCanvas().addPlayer(player);
	}

	@Override
	public void onDestroy(GameCloseReason reason) {
		this.display.destroy();
		this.display.getCanvas().destroy();
	}

	@Override
	public void onTick() {
		if (this.mainPlayer == null) return;

		// Decrease ticks until game end to zero
		if (this.ticksUntilClose >= 0) {
			if (this.ticksUntilClose == 0) {
				this.gameSpace.close(GameCloseReason.FINISHED);
			}

			this.ticksUntilClose -= 1;
			return;
		}

		SlideDirection direction = SlideDirection.fromPlayer(this.mainPlayer);

		if (SlideDirection.isDifferent(direction, this.lastSlideDirection)) {
			this.lastSlideDirection = direction;

			if (SlideDirection.isDeterminate(direction)) {
				BoardChange change = this.board.slide(direction);

				if (change != BoardChange.NONE) {
					this.board.render();
					this.bar.updateTitle();

					this.sendSound(this.config.soundConfig().slide());

					if (change == BoardChange.MERGE) {
						this.sendSound(this.config.soundConfig().merge());
					}
				}

				if (!this.board.isMovePossible()) {
					this.endGame();
				}
			}
		}
	}

	@Override
	public JoinAcceptorResult onAcceptPlayers(JoinAcceptor acceptor) {
		if (this.mainPlayer == null) {
			Vec3 spawnPos = this.board.getSpawnPos();

			return acceptor.teleport(this.world, spawnPos).thenRunForEach(player -> {
				this.mainPlayer = player;

				this.spawnMount(spawnPos, this.mainPlayer);
				this.initializePlayer(this.mainPlayer, GameType.ADVENTURE);
			});
		} else {
			Vec3 pos = this.mainPlayer.position().add(this.config.spectatorSpawnOffset());
			return acceptor.teleport(this.world, pos).thenRunForEach(player -> {
				player.setYRot(this.mainPlayer.getYRot());
				player.setXRot(this.mainPlayer.getXRot());

				this.initializePlayer(player, GameType.SPECTATOR);
			});
		}
	}

	@Override
	public JoinOfferResult onOfferPlayers(JoinOffer offer) {
		return this.mainPlayer == null ? offer.acceptParticipants() : offer.acceptSpectators();
	}

	@Override
	public EventResult onDamage(ServerPlayer player, DamageSource source, float damage) {
		return EventResult.DENY;
	}

	@Override
	public EventResult onDeath(ServerPlayer player, DamageSource source) {
		if (player == this.mainPlayer) {
			this.endGame();
		}
		return EventResult.DENY;
	}

	@Override
	public void onRemovePlayer(ServerPlayer player) {
		this.display.removePlayer(player);
		this.display.getCanvas().removePlayer(player);

		if (player == this.mainPlayer) {
			this.gameSpace.close(GameCloseReason.FINISHED);
		}
	}

	// Utilities
	private void sendSound(SoundEvent sound) {
		this.gameSpace.getPlayers().playSound(sound, SoundSource.PLAYERS, this.config.soundConfig().volume(), this.config.soundConfig().pitch());
	}

	private Entity spawnMount(Vec3 playerPos, ServerPlayer player) {
		Mob mount = EntityTypes.MULE.create(this.world, EntitySpawnReason.JOCKEY);

		double y = playerPos.y() - mount.getPassengerRidingPosition(player).y() - player.getEyeHeight(Pose.SITTING);
		mount.setPosRaw(playerPos.x(), y, playerPos.z());
		mount.setYHeadRot(this.board.getSpawnAngle());
		mount.setYRot(this.board.getSpawnAngle());

		mount.setNoAi(true);
		mount.setNoGravity(true);
		mount.setSilent(true);
		mount.setPersistenceRequired();

		// Prevent mount from being visible
		mount.addEffect(this.createInfiniteStatusEffect(MobEffects.INVISIBILITY));
		mount.setInvisible(true);

		// Remove mount hearts from HUD
		mount.getAttribute(Attributes.MAX_HEALTH).setBaseValue(0);

		this.world.addFreshEntity(mount);
		player.startRiding(mount, true, false);

		return null;
	}

	private void initializePlayer(ServerPlayer player, GameType gameMode) {
		player.setGameMode(gameMode);
		player.addEffect(this.createInfiniteStatusEffect(MobEffects.NIGHT_VISION));
	}

	private MobEffectInstance createInfiniteStatusEffect(Holder<MobEffect> effect) {
		return new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, 0, true, false);
	}

	private void endGame() {
		this.ticksUntilClose = this.config.ticksUntilClose();

		Component message = Component.translatable("text.compound.reached_score", this.mainPlayer.getDisplayName(), this.board.getScore()).withStyle(ChatFormatting.GOLD);
		this.gameSpace.getPlayers().sendMessage(message);

		this.sendSound(this.config.soundConfig().gameEnd());
	}

	public int getScore() {
		return this.board.getScore();
	}
}