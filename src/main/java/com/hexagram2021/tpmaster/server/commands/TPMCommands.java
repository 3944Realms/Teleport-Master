package com.hexagram2021.tpmaster.server.commands;

import com.hexagram2021.tpmaster.server.config.TPMServerConfig;
import com.hexagram2021.tpmaster.server.util.ITeleportable;
import com.hexagram2021.tpmaster.server.util.LevelUtils;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.commands.TeleportCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

public class TPMCommands {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		List<LiteralArgumentBuilder<CommandSourceStack>> commandList = new ArrayList<>();
		Command<CommandSourceStack> Tpa_accept = context -> accept(context.getSource(), context.getSource().getEntityOrException());

		commandList.add(
				Commands.literal("tpaccept").requires(stack -> stack.hasPermission(TPMServerConfig.ACCEPT_DENY_PERMISSION_LEVEL.get()))
				.executes(Tpa_accept)
		);

		Command<CommandSourceStack> Tpa_deny = context -> deny(context.getSource().getEntityOrException());
		commandList.add(Commands.literal("tpadeny").requires(stack -> stack.hasPermission(TPMServerConfig.ACCEPT_DENY_PERMISSION_LEVEL.get()))
				.executes(Tpa_deny));

		Command<CommandSourceStack> Tpa_away = context -> away(context.getSource(), context.getSource().getEntityOrException(), 0, true, null);
		Command<CommandSourceStack> Tpa_away_distance = context -> away(context.getSource(), context.getSource().getEntityOrException(), context.getArgument("distance", Integer.class), true, null);
		Command<CommandSourceStack> Tpa_away_distance_mustOnLand = context -> away(context.getSource(), context.getSource().getEntityOrException(), context.getArgument("distance", Integer.class), context.getArgument("mustOnLand", Boolean.class), null);
		commandList.add(
				Commands.literal("rtp").requires(stack -> stack.hasPermission(TPMServerConfig.AWAY_PERMISSION_LEVEL.get()))
						.executes(Tpa_away)
						.then(
								Commands.argument("distance", IntegerArgumentType.integer(0, 10000))
										.executes(Tpa_away_distance)
										.then(
												Commands.argument("mustOnLand", BoolArgumentType.bool())
														.executes(Tpa_away_distance_mustOnLand)
										)
						)
		);
		Command<CommandSourceStack> Tpa_tpa = context -> request(context.getSource(), context.getSource().getEntityOrException(), EntityArgument.getEntity(context, "target"), ITeleportable.RequestType.ASK);
		Command<CommandSourceStack> Tpa_tpa_here = context -> request(context.getSource(), context.getSource().getEntityOrException(), EntityArgument.getEntity(context, "target"), ITeleportable.RequestType.INVITE);
		commandList.add(
				Commands.literal("tpa").requires(stack -> stack.hasPermission(TPMServerConfig.REQUEST_PERMISSION_LEVEL.get()))
						.then(
								Commands.argument("target", EntityArgument.entity())
										.executes(Tpa_tpa)
										.then(
												Commands.literal("here")
														.executes(Tpa_tpa_here)
										)
						)
		);
		Command<CommandSourceStack> Tpa_spawn = context -> spawn(context.getSource(), context.getSource().getEntityOrException());
		commandList.add(
				Commands.literal("tpaspawn").requires(stack -> stack.hasPermission(TPMServerConfig.SPAWN_PERMISSION_LEVEL.get()))
						.executes(Tpa_spawn)
		);
		Command<CommandSourceStack> Tpa_setHome = context -> sethome(context.getSource().getEntityOrException(), 0);
		Command<CommandSourceStack> Tpa_setHome_index = context -> sethome(context.getSource().getEntityOrException(), IntegerArgumentType.getInteger(context, "index"));
		commandList.add(
				Commands.literal("sethome").requires(stack -> stack.hasPermission(TPMServerConfig.HOME_PERMISSION_LEVEL.get()))
						.executes(Tpa_setHome)
						.then(
								Commands.argument("index", IntegerArgumentType.integer(0, TPMServerConfig.MAX_HOME_COUNT.get() - 1))
										.executes(Tpa_setHome_index)
						)
		);
		Command<CommandSourceStack> Tpa_home = context -> home(context.getSource(), context.getSource().getEntityOrException(), 0);
		Command<CommandSourceStack> Tpa_home_index = context -> home(context.getSource(), context.getSource().getEntityOrException(), IntegerArgumentType.getInteger(context, "index"));
		commandList.add(
				Commands.literal("home").requires(stack -> stack.hasPermission(TPMServerConfig.HOME_PERMISSION_LEVEL.get()))
						.executes(Tpa_home)
						.then(
								Commands.argument("index", IntegerArgumentType.integer(0, TPMServerConfig.MAX_HOME_COUNT.get() - 1))
										.executes(Tpa_home_index)
						)
		);
		Command<CommandSourceStack> Tpa_back = context -> back(context.getSource(), context.getSource().getEntityOrException());
		commandList.add(
				Commands.literal("back").requires(stack -> stack.hasPermission(TPMServerConfig.BACK_PERMISSION_LEVEL.get()))
						.executes(Tpa_back)
		);
		Command<CommandSourceStack> Tpa_del_home_index = context -> removeHome(context.getSource().getEntityOrException(), IntegerArgumentType.getInteger(context, "index"));
		Command<CommandSourceStack> Tpa_del_back = context -> removeBack(context.getSource().getEntityOrException());
		commandList.add(
				Commands.literal("delete").requires(stack -> stack.hasPermission(TPMServerConfig.REMOVE_PERMISSION_LEVEL.get()))
						.then(
								Commands.literal("home").then(
										Commands.argument("index", IntegerArgumentType.integer(0, TPMServerConfig.MAX_HOME_COUNT.get() - 1))
												.executes(Tpa_del_home_index)
								)
						)
						.then(
								Commands.literal("back").executes(Tpa_del_back)
						)
		);
		Command<CommandSourceStack> Tpa_help = context -> help(context.getSource().getEntityOrException());
		commandList.add(
				Commands.literal("tpaHelp").requires(stack -> stack.hasPermission(TPMServerConfig.HELP_PERMISSION_LEVEL.get()))
						.executes(Tpa_help)
		);

		commandList.forEach(dispatcher::register);
	}

	private static final SimpleCommandExceptionType NO_NEED_TO_ACCEPT = new SimpleCommandExceptionType(
			new TranslatableComponent("commands.tpmaster.accept.failed.no_request")
	);
	private static final SimpleCommandExceptionType NO_NEED_TO_DENY = new SimpleCommandExceptionType(
			new TranslatableComponent("commands.tpmaster.deny.failed.no_request")
	);
	private static final DynamicCommandExceptionType TARGET_UNHANDLED_RESERVATION = new DynamicCommandExceptionType(
			(name) -> new TranslatableComponent("commands.tpmaster.request.failed.reserved", name)
	);

	private static final DynamicCommandExceptionType INVALID_AWAY_DISTANCE_PARAMETER = new DynamicCommandExceptionType(
			(d) -> new TranslatableComponent("commands.tpmaster.away.invalid.distance", d)
	);
	private static final SimpleCommandExceptionType CANNOT_FIND_POSITION = new SimpleCommandExceptionType(
			new TranslatableComponent("commands.tpmaster.away.failed.no_position")
	);
	public static final Dynamic2CommandExceptionType INVALID_SETHOME_INDEX_PARAMETER = new Dynamic2CommandExceptionType(
			(i, max) -> new TranslatableComponent("commands.tpmaster.sethome.invalid.index", i, max)
	);
	private static final DynamicCommandExceptionType NO_HOME_TO_HOME = new DynamicCommandExceptionType(
			(d) -> new TranslatableComponent("commands.tpmaster.home.failed.no_home", d)
	);
	private static final DynamicCommandExceptionType NO_LEVEL_FOUNDED_TO_HOME = new DynamicCommandExceptionType(
			(level) -> new TranslatableComponent("commands.tpmaster.home.failed.no_level", level)
	);
	private static final SimpleCommandExceptionType NO_DEATH_POINT_TO_BACK = new SimpleCommandExceptionType(
			new TranslatableComponent("commands.tpmaster.back.failed.no_home")
	);
	private static final DynamicCommandExceptionType NO_LEVEL_FOUNDED_TO_BACK = new DynamicCommandExceptionType(
			(level) -> new TranslatableComponent("commands.tpmaster.back.failed.no_level", level)
	);

	private static final DynamicCommandExceptionType COOL_DOWN_AWAY = new DynamicCommandExceptionType(
			(d) -> new TranslatableComponent("commands.tpmaster.away.failed.cool_down", d)
	);
	private static final DynamicCommandExceptionType COOL_DOWN_REQUEST = new DynamicCommandExceptionType(
			(d) -> new TranslatableComponent("commands.tpmaster.request.failed.cool_down", d)
	);

	private static int accept(CommandSourceStack stack, Entity entity) throws CommandSyntaxException {
		if(entity instanceof ITeleportable teleportable) {
			Entity requester = teleportable.getTeleportMasterRequester();
			ITeleportable.RequestType requestType = teleportable.getRequestType();
			if(requester == null || requestType == null) {
				throw NO_NEED_TO_ACCEPT.create();
			}

			switch(requestType) {
				case ASK -> TeleportCommand.performTeleport(
						stack, requester, (ServerLevel)entity.level,
						entity.getX(), entity.getY(), entity.getZ(),
						EnumSet.noneOf(ClientboundPlayerPositionPacket.RelativeArgument.class),
						requester.getYRot(), requester.getXRot(), null
				);
				case INVITE -> TeleportCommand.performTeleport(
						stack, entity, (ServerLevel)requester.level,
						requester.getX(), requester.getY(), requester.getZ(),
						EnumSet.noneOf(ClientboundPlayerPositionPacket.RelativeArgument.class),
						entity.getYRot(), entity.getXRot(), null
				);
			}

			entity.sendMessage(new TranslatableComponent("commands.tpmaster.accept.success", requester.getName().getString()), Util.NIL_UUID);
			requester.sendMessage(new TranslatableComponent("commands.tpmaster.request.accepted", entity.getName().getString()), Util.NIL_UUID);
			teleportable.clearTeleportMasterRequest();
		}
		return Command.SINGLE_SUCCESS;
	}

	public static int deny(Entity entity) throws CommandSyntaxException {
		if(entity instanceof ITeleportable teleportable) {
			Entity requester = teleportable.getTeleportMasterRequester();
			if(requester == null) {
				throw NO_NEED_TO_DENY.create();
			}

			entity.sendMessage(new TranslatableComponent("commands.tpmaster.deny.success", requester.getName().getString()), Util.NIL_UUID);
			requester.sendMessage(new TranslatableComponent("commands.tpmaster.request.denied", entity.getName().getString()), Util.NIL_UUID);
			teleportable.clearTeleportMasterRequest();
		}
		return Command.SINGLE_SUCCESS;
	}

	@SuppressWarnings("SameParameterValue")
	private static int away(CommandSourceStack stack, Entity entity, int distance, boolean mustOnLand, @Nullable TeleportCommand.LookAt lookAt) throws CommandSyntaxException {
		if(entity instanceof ITeleportable teleportable) {
			if(!teleportable.canUseTeleportMasterAway()) {
				throw COOL_DOWN_AWAY.create(teleportable.getTeleportMasterAwayCoolDownTick() / 20);
			}
			teleportable.setTeleportMasterAway();
		}
		if(distance == 0) {
			distance = entity.level.getRandom().nextInt(600) + 600;
		} else if(distance < 0 || distance > 10000) {
			throw INVALID_AWAY_DISTANCE_PARAMETER.create(distance);
		}

		boolean flag = false;
		Random random = entity.level.getRandom();
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();
		for(int i = 0; i < TPMServerConfig.AWAY_TRY_COUNT.get(); ++i) {
			double phi = random.nextDouble() * 2.0D * Math.acos(-1.0D);
			x = entity.getX() + distance * Math.cos(phi) + random.nextDouble() * TPMServerConfig.AWAY_NOISE_BOUND.get() * distance;
			z = entity.getZ() + distance * Math.sin(phi) + random.nextDouble() * TPMServerConfig.AWAY_NOISE_BOUND.get() * distance;
			BlockPos blockPos = new BlockPos(x, 255.0D, z);
			Biome biome = entity.level.getBiome(blockPos).value();
			boolean conti = false;
			if(mustOnLand) {
				for (String ocean : TPMServerConfig.OCEAN_BIOME_KEYS.get()) {
					ResourceLocation biomeId = ForgeRegistries.BIOMES.getKey(biome);
					if (biomeId != null && biomeId.toString().equals(ocean)) {
						conti = true;
						break;
					}
				}
			}
			if(!conti) {
				flag = true;
				y = LevelUtils.getTopBlock(entity.level, blockPos);
				if(y < 8) {
					continue;
				}
				break;
			}
		}
		if(!flag) {
			throw CANNOT_FIND_POSITION.create();
		}
		TeleportCommand.performTeleport(stack, entity, (ServerLevel)entity.level, x, y, z, EnumSet.noneOf(ClientboundPlayerPositionPacket.RelativeArgument.class), entity.getYRot(), entity.getXRot(), lookAt);

		entity.sendMessage(new TranslatableComponent("commands.tpmaster.away.success", distance), Util.NIL_UUID);

		return Command.SINGLE_SUCCESS;
	}

	private static int request(CommandSourceStack stack, Entity entity, Entity target, ITeleportable.RequestType type) throws CommandSyntaxException {
		if(entity instanceof ITeleportable teleportable) {
			if (!teleportable.canUseTeleportMasterRequest()) {
				throw COOL_DOWN_REQUEST.create(teleportable.getTeleportMasterRequestCoolDownTick() / 20);
			}
		}
		if(target instanceof ITeleportable teleportableTarget) {
			if(teleportableTarget.getTeleportMasterRequester() != null) {
				throw TARGET_UNHANDLED_RESERVATION.create(target.getName().getString());
			}
			if(entity instanceof ITeleportable teleportable) {
				teleportable.setTeleportMasterRequest(teleportableTarget, type);
			} else {
				teleportableTarget.receiveTeleportMasterRequestFrom(entity, type);
			}

			entity.sendMessage(new TranslatableComponent("commands.tpmaster.request.success", target.getName().getString()), Util.NIL_UUID);
			target.sendMessage(new TranslatableComponent(
					switch(type) {
						case ASK -> "commands.tpmaster.request.receive.ask";
						case INVITE -> "commands.tpmaster.request.receive.invite";
					},
					entity.getName().getString(), TPMServerConfig.REQUEST_COMMAND_AUTO_DENY_TICK.get() / 20
			), Util.NIL_UUID);
		} else {
			entity.sendMessage(new TranslatableComponent("commands.tpmaster.request.success", target.getName().getString()), Util.NIL_UUID);
			boolean flag1 = entity instanceof Monster || (entity instanceof NeutralMob && !(entity instanceof TamableAnimal));
			boolean flag2 = target instanceof Monster || (target instanceof NeutralMob && !(target instanceof TamableAnimal));
			if(flag1 == flag2) {
				switch(type) {
					case ASK -> TeleportCommand.performTeleport(
							stack, entity, (ServerLevel)target.level,
							target.getX(), target.getY(), target.getZ(),
							EnumSet.noneOf(ClientboundPlayerPositionPacket.RelativeArgument.class),
							entity.getYRot(), entity.getXRot(), null
					);
					case INVITE -> TeleportCommand.performTeleport(
							stack, target, (ServerLevel)entity.level,
							entity.getX(), entity.getY(), entity.getZ(),
							EnumSet.noneOf(ClientboundPlayerPositionPacket.RelativeArgument.class),
							target.getYRot(), target.getXRot(), null
					);
				}
				entity.sendMessage(new TranslatableComponent("commands.tpmaster.request.accepted", target.getName().getString()), Util.NIL_UUID);
			} else {
				entity.sendMessage(new TranslatableComponent("commands.tpmaster.request.denied", target.getName().getString()), Util.NIL_UUID);
			}
		}

		return Command.SINGLE_SUCCESS;
	}

	private static int spawn(CommandSourceStack stack, Entity entity) throws CommandSyntaxException {
		ServerLevel overworld = stack.getServer().overworld();
		BlockPos spawnPoint = overworld.getSharedSpawnPos();
		TeleportCommand.performTeleport(
				stack, entity, overworld,
				spawnPoint.getX(), spawnPoint.getY() + 1.0D, spawnPoint.getZ(),
				EnumSet.noneOf(ClientboundPlayerPositionPacket.RelativeArgument.class),
				entity.getYRot(), entity.getXRot(), null
		);

		return Command.SINGLE_SUCCESS;
	}

	private static int sethome(Entity entity, int index) throws  CommandSyntaxException {
		if(entity instanceof ITeleportable teleportable) {
			BlockPos pos = entity.getOnPos();
			GlobalPos globalPos = GlobalPos.of(entity.level.dimension(), entity.getOnPos());
			teleportable.setTeleportMasterHome(globalPos, index);
			entity.sendMessage(new TranslatableComponent("commands.tpmaster.sethome.success", pos.getX(), pos.getY(), pos.getZ(), index), Util.NIL_UUID);
		}
		return Command.SINGLE_SUCCESS;
	}

	private static int home(CommandSourceStack stack, Entity entity, int index) throws CommandSyntaxException {
		if(entity instanceof ITeleportable teleportable) {
			GlobalPos globalPos = teleportable.getTeleportMasterHome(index);
			if(globalPos == null) {
				throw NO_HOME_TO_HOME.create(index);
			}
			ServerLevel level = stack.getServer().getLevel(globalPos.dimension());
			if(level == null) {
				throw NO_LEVEL_FOUNDED_TO_HOME.create(globalPos.dimension().toString());
			}
			BlockPos pos = globalPos.pos();
			TeleportCommand.performTeleport(
					stack, entity, level,
					pos.getX(), pos.getY() + 1.0D, pos.getZ(),
					EnumSet.noneOf(ClientboundPlayerPositionPacket.RelativeArgument.class),
					entity.getYRot(), entity.getXRot(), null
			);
		}

		return Command.SINGLE_SUCCESS;
	}

	private static int back(CommandSourceStack stack, Entity entity) throws CommandSyntaxException {
		if(entity instanceof ITeleportable teleportable) {
			GlobalPos globalPos = teleportable.getTeleportMasterLastDeathPoint();
			if(globalPos == null) {
				throw NO_DEATH_POINT_TO_BACK.create();
			}
			ServerLevel level = stack.getServer().getLevel(globalPos.dimension());
			if(level == null) {
				throw NO_LEVEL_FOUNDED_TO_BACK.create(globalPos.dimension().toString());
			}
			BlockPos pos = globalPos.pos();
			TeleportCommand.performTeleport(
					stack, entity, level,
					pos.getX(), pos.getY() + 1.0D, pos.getZ(),
					EnumSet.noneOf(ClientboundPlayerPositionPacket.RelativeArgument.class),
					entity.getYRot(), entity.getXRot(), null
			);
		}
		return Command.SINGLE_SUCCESS;
	}

	private static int removeHome(Entity entity, int index) throws CommandSyntaxException {
		if(entity instanceof ITeleportable teleportable) {
			teleportable.setTeleportMasterHome(null, index);
			entity.sendMessage(new TranslatableComponent("commands.tpmaster.remove.home.success", index), Util.NIL_UUID);
		}
		return Command.SINGLE_SUCCESS;
	}

	private static int removeBack(Entity entity) {
		if(entity instanceof ITeleportable teleportable) {
			teleportable.setTeleportMasterLastDeathPoint(null);
			entity.sendMessage(new TranslatableComponent("commands.tpmaster.remove.back.success"), Util.NIL_UUID);
		}
		return Command.SINGLE_SUCCESS;
	}

	private static int help(Entity entity) {
		entity.sendMessage(new TranslatableComponent("commands.tpmaster.help"), Util.NIL_UUID);

		return Command.SINGLE_SUCCESS;
	}
}
