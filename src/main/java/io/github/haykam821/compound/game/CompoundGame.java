package io.github.haykam821.compound.game;

import eu.pb4.mapcanvas.api.utils.VirtualDisplay;
import io.github.haykam821.compound.game.board.Board;
import io.github.haykam821.compound.game.board.BoardChange;
import io.github.haykam821.compound.game.board.SlideDirection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.util.VoidChunkGenerator;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class CompoundGame implements GamePlayerEvents.Add, GameActivityEvents.Destroy, GameActivityEvents.Tick, GamePlayerEvents.Remove, GamePlayerEvents.Offer, PlayerDamageEvent, PlayerDeathEvent {
	private final GameSpace gameSpace;
	private final ServerWorld world;
	private final CompoundConfig config;

	private final Board board;
	private final ScoreBar bar;
	private final VirtualDisplay display;

	private ServerPlayerEntity mainPlayer;
	private SlideDirection lastSlideDirection;

	private int ticksUntilClose = -1;

	public CompoundGame(GameSpace gameSpace, ServerWorld world, CompoundConfig config, Board board, GlobalWidgets widgets, VirtualDisplay display) {
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
		Board board = new Board(config.boardConfig(), context.server().getOverworld().getRandom());

		RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
			.setGenerator(new VoidChunkGenerator(context.server().getRegistryManager().get(RegistryKeys.BIOME)));

		return context.openWithWorld(worldConfig, (activity, world) -> {
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
			activity.listen(GamePlayerEvents.OFFER, phase);
			activity.listen(PlayerDamageEvent.EVENT, phase);
			activity.listen(PlayerDeathEvent.EVENT, phase);
			activity.listen(GamePlayerEvents.REMOVE, phase);
		});
	}

	// Listeners
	@Override
	public void onAddPlayer(ServerPlayerEntity player) {
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

		SlideDirection direction = SlideDirection.fromMovement(this.mainPlayer);

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
	public PlayerOfferResult onOfferPlayer(PlayerOffer offer) {
		if (this.mainPlayer == null) {
			Vec3d spawnPos = this.board.getSpawnPos();

			return offer.accept(this.world, spawnPos).and(() -> {
				this.mainPlayer = offer.player();

				this.spawnMount(spawnPos, this.mainPlayer);
				this.initializePlayer(this.mainPlayer, GameMode.ADVENTURE);
			});
		} else {
			Vec3d pos = this.mainPlayer.getPos().add(this.config.spectatorSpawnOffset());
			return offer.accept(this.world, pos).and(() -> {
				offer.player().setYaw(this.mainPlayer.getYaw());
				offer.player().setPitch(this.mainPlayer.getPitch());

				this.initializePlayer(offer.player(), GameMode.SPECTATOR);
			});
		}
	}

	@Override
	public ActionResult onDamage(ServerPlayerEntity player, DamageSource source, float damage) {
		return ActionResult.FAIL;
	}

	@Override
	public ActionResult onDeath(ServerPlayerEntity player, DamageSource source) {
		if (player == this.mainPlayer) {
			this.endGame();
		}
		return ActionResult.FAIL;
	}

	@Override
	public void onRemovePlayer(ServerPlayerEntity player) {
		this.display.removePlayer(player);
		this.display.getCanvas().removePlayer(player);

		if (player == this.mainPlayer) {
			this.gameSpace.close(GameCloseReason.FINISHED);
		}
	}

	// Utilities
	private void sendSound(SoundEvent sound) {
		this.gameSpace.getPlayers().playSound(sound, SoundCategory.PLAYERS, this.config.soundConfig().volume(), this.config.soundConfig().pitch());
	}

	private Entity spawnMount(Vec3d playerPos, ServerPlayerEntity player) {
		MobEntity mount = EntityType.MULE.create(this.world);

		double y = playerPos.getY() - mount.getMountedHeightOffset() - player.getHeightOffset() + player.getY() - player.getEyeY();
		mount.setPos(playerPos.getX(), y, playerPos.getZ());
		mount.setYaw(this.board.getSpawnAngle());

		mount.setAiDisabled(true);
		mount.setNoGravity(true);
		mount.setSilent(true);
		mount.setPersistent();

		// Prevent mount from being visible
		mount.addStatusEffect(this.createInfiniteStatusEffect(StatusEffects.INVISIBILITY));
		mount.setInvisible(true);

		// Remove mount hearts from HUD
		mount.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(0);

		this.world.spawnEntity(mount);
		player.startRiding(mount, true);

		return null;
	}

	private void initializePlayer(ServerPlayerEntity player, GameMode gameMode) {
		player.changeGameMode(gameMode);
		player.addStatusEffect(this.createInfiniteStatusEffect(StatusEffects.NIGHT_VISION));
	}

	private StatusEffectInstance createInfiniteStatusEffect(StatusEffect statusEffect) {
		return new StatusEffectInstance(statusEffect, Integer.MAX_VALUE, 0, true, false);
	}

	private void endGame() {
		this.ticksUntilClose = this.config.ticksUntilClose();

		Text message = Text.translatable("text.compound.reached_score", this.mainPlayer.getDisplayName(), this.board.getScore()).formatted(Formatting.GOLD);
		this.gameSpace.getPlayers().sendMessage(message);

		this.sendSound(this.config.soundConfig().gameEnd());
	}

	public int getScore() {
		return this.board.getScore();
	}
}