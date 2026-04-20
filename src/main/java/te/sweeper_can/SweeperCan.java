package te.sweeper_can;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SweeperCan implements ModInitializer {
	public static final String MOD_ID = "sweeper-can";

	public static final ResourceLocation OPEN_CONFIG_PACKET = new ResourceLocation(MOD_ID, "open_config");
	public static final ResourceLocation SYNC_CONFIG_PACKET = new ResourceLocation(MOD_ID, "sync_config");
	public static final ResourceLocation SAVE_CONFIG_PACKET = new ResourceLocation(MOD_ID, "save_config");

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static int tickCounter = 0;

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		SweeperConfig.loadConfig();

		// Register /sweeper command
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			var openNode = Commands.literal("open")
				.executes(context -> {
					var player = context.getSource().getPlayerOrException();
					var state = SweeperState.getServerState(context.getSource().getServer());
					player.openMenu(new SimpleMenuProvider(
							(syncId, inventory, p) -> ChestMenu.sixRows(syncId, inventory, state.inventories[0]),
							Component.literal(SweeperConfig.INSTANCE.trashCanTitle + " (Page 1)")
					));
					return 1;
				})
				.then(Commands.argument("page", IntegerArgumentType.integer(1, 10)).executes(context -> {
					int page = IntegerArgumentType.getInteger(context, "page") - 1;
					var player = context.getSource().getPlayerOrException();
					var state = SweeperState.getServerState(context.getSource().getServer());
					int maxPages = Math.min(10, Math.max(1, SweeperConfig.INSTANCE.trashCanCount));
					if (page >= maxPages) page = maxPages - 1;
					final int finalPage = page;
					String title = SweeperConfig.INSTANCE.trashCanTitle;
					if (maxPages > 1) {
					    title += " (Page " + (finalPage + 1) + ")";
					}
					player.openMenu(new SimpleMenuProvider(
							(syncId, inventory, p) -> ChestMenu.sixRows(syncId, inventory, state.inventories[finalPage]),
							Component.literal(title)
					));
					return 1;
				}));

			dispatcher.register(Commands.literal("sweeper")
				.then(openNode)
                                .then(Commands.literal("manual").requires(source -> source.hasPermission(SweeperConfig.INSTANCE.commandOpLevel)).executes(context -> {
                                        int interval = SweeperConfig.INSTANCE.intervalTicks;
					tickCounter = interval - 200; // 10 seconds before clear
					return 1;
				}))
                                .then(Commands.literal("amount").requires(source -> source.hasPermission(SweeperConfig.INSTANCE.commandOpLevel))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 10)).executes(context -> {
						int newCount = IntegerArgumentType.getInteger(context, "count");
						SweeperConfig.INSTANCE.trashCanCount = newCount;
						SweeperConfig.saveConfig();
						syncConfigToAll(context.getSource().getServer());
						context.getSource().sendSuccess(() -> Component.literal(String.format(SweeperConfig.INSTANCE.messageAmountSet, newCount)), true);
						return 1;
					}))
				)
                                .then(Commands.literal("config").requires(source -> source.hasPermission(SweeperConfig.INSTANCE.commandOpLevel)).executes(context -> {
                                        ServerPlayer player = context.getSource().getPlayerOrException();
                                        if (ServerPlayNetworking.canSend(player, OPEN_CONFIG_PACKET)) {
                                                ServerPlayNetworking.send(player, OPEN_CONFIG_PACKET, PacketByteBufs.create());
                                        } else {
                                                context.getSource().sendFailure(Component.literal("You need the Sweeper Can mod installed on the client to use the config UI."));
                                        }
                                        return 1;
                                }))
			);
		});

		// Networking: Send config on join
                ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
                        if (ServerPlayNetworking.canSend(handler.getPlayer(), SYNC_CONFIG_PACKET)) {
                                FriendlyByteBuf buf = PacketByteBufs.create();
                                buf.writeUtf(SweeperConfig.serialize());
                                ServerPlayNetworking.send(handler.getPlayer(), SYNC_CONFIG_PACKET, buf);
                        }
                });

		// Networking: Save Config from Client
		ServerPlayNetworking.registerGlobalReceiver(SAVE_CONFIG_PACKET, (server, player, handler, buf, responseSender) -> {
			if (player.hasPermissions(2)) {
				String json = buf.readUtf();
				server.execute(() -> {
					SweeperConfig.deserialize(json);
					SweeperConfig.saveConfig();
					syncConfigToAll(server);
				});
			}
		});

		// Check every tick
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			tickCounter++;
			int interval = SweeperConfig.INSTANCE.intervalTicks;

			if (tickCounter == interval - 1200) { // 60 seconds
				Component msg = Component.literal(SweeperConfig.INSTANCE.message60s);
				for (ServerPlayer p : server.getPlayerList().getPlayers()) { p.displayClientMessage(msg, true); }
			} else if (tickCounter == interval - 600) { // 30 seconds
				Component msg = Component.literal(SweeperConfig.INSTANCE.message30s);
				for (ServerPlayer p : server.getPlayerList().getPlayers()) { p.displayClientMessage(msg, true); }
			} else if (tickCounter > interval - 200 && tickCounter < interval) {
				if (tickCounter % 20 == 0) {
					int secondsLeft = (interval - tickCounter) / 20;
					Component msg = Component.literal(String.format(SweeperConfig.INSTANCE.messageCountdown, secondsLeft));
					for (ServerPlayer p : server.getPlayerList().getPlayers()) { p.displayClientMessage(msg, true); }
				}
			} else if (tickCounter >= interval) {
				// Time to clear
				var state = SweeperState.getServerState(server);
				int clearedCount = 0;

				for (ServerLevel level : server.getAllLevels()) {
					for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
						if (entity instanceof ItemEntity itemEntity) {
							ItemStack stack = itemEntity.getItem();
							clearedCount += stack.getCount();

							// Try to insert into trash can pages
							int maxPages = Math.min(10, Math.max(1, SweeperConfig.INSTANCE.trashCanCount));
							outer: for (int page = 0; page < maxPages; page++) {
								for (int i = 0; i < state.inventories[page].getContainerSize(); i++) {
									if (stack.isEmpty()) break outer;
									ItemStack currentSlot = state.inventories[page].getItem(i);
									if (currentSlot.isEmpty()) {
										state.inventories[page].setItem(i, stack.copy());
										stack.setCount(0);
										break outer;
									} else if (ItemStack.isSameItemSameTags(currentSlot, stack)) {
										int space = currentSlot.getMaxStackSize() - currentSlot.getCount();
										int transfer = Math.min(space, stack.getCount());
										if (transfer > 0) {
											currentSlot.grow(transfer);
											stack.shrink(transfer);
										}
									}
								}
							}
							
							// Even if it didn't fit in the trash can, discard the dropped item.
							itemEntity.discard();
						}
					}
				}
				Component msg = Component.literal(String.format(SweeperConfig.INSTANCE.messageCleared, clearedCount));
				for (ServerPlayer p : server.getPlayerList().getPlayers()) { p.displayClientMessage(msg, true); }
				tickCounter = 0;
			}
		});

		LOGGER.info("Hello Fabric world!");
	}

        public static void syncConfigToAll(net.minecraft.server.MinecraftServer server) {
                FriendlyByteBuf buf = PacketByteBufs.create();
                buf.writeUtf(SweeperConfig.serialize());
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                        if (ServerPlayNetworking.canSend(player, SYNC_CONFIG_PACKET)) {
                                ServerPlayNetworking.send(player, SYNC_CONFIG_PACKET, buf);
                        }
                }
        }
}

