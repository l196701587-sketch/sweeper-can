package te.sweeper_can.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import te.sweeper_can.SweeperConfig;
import te.sweeper_can.SweeperCan;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;

public class SweeperCanClient implements ClientModInitializer {
    private static double savedMouseX = -1;
    private static double savedMouseY = -1;
    private static boolean expectsPageSwitch = false;

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(SweeperCan.SYNC_CONFIG_PACKET, (client, handler, buf, responseSender) -> {
			String json = buf.readUtf();
			client.execute(() -> SweeperConfig.deserialize(json));
		});

		ClientPlayNetworking.registerGlobalReceiver(SweeperCan.OPEN_CONFIG_PACKET, (client, handler, buf, responseSender) -> {
			client.execute(() -> {
				Minecraft.getInstance().setScreen(buildConfigScreen(Minecraft.getInstance().screen));
			});
		});

		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			boolean shouldShow = false;
			String mode = SweeperConfig.INSTANCE.showInventoryButton.toLowerCase();

			if (screen instanceof InventoryScreen) { // 生存模式背包
				if ("all".equals(mode) || "survival".equals(mode)) shouldShow = true;
			} else if (screen instanceof CreativeModeInventoryScreen) { // 创造模式背包
				if ("all".equals(mode) || "creative".equals(mode)) shouldShow = true;
			}

			if (shouldShow) {
				Screens.getButtons(screen).add(Button.builder(Component.literal("🗑"), btn -> {
					Minecraft.getInstance().player.connection.sendUnsignedCommand("sweeper open");
				}).bounds(
					scaledWidth / 2 - 10 + SweeperConfig.INSTANCE.inventoryButtonX,
					5 + SweeperConfig.INSTANCE.inventoryButtonY,
					40,
					20
				).build());
			}

			if (screen instanceof me.shedaniel.clothconfig2.gui.ClothConfigScreen) {
				Screens.getButtons(screen).add(Button.builder(Component.literal("Open Blacklist / 清理黑名单"), btn -> {
					Minecraft.getInstance().setScreen(null); // Close config UI first
					if (Minecraft.getInstance().player != null) {
						Minecraft.getInstance().player.connection.sendUnsignedCommand("sweeper blacklist");
					}
				}).bounds(10, 10, 140, 20).build());
				// 去除搜索框
				try {
					java.lang.reflect.Field field = me.shedaniel.clothconfig2.gui.ClothConfigScreen.class.getDeclaredField("searchFieldEntry");
					field.setAccessible(true);
					Object searchEntry = field.get(screen);
					if (searchEntry != null) {
						java.lang.reflect.Field editBoxField = me.shedaniel.clothconfig2.gui.widget.SearchFieldEntry.class.getDeclaredField("editBox");
						editBoxField.setAccessible(true);
						net.minecraft.client.gui.components.EditBox editBox = (net.minecraft.client.gui.components.EditBox) editBoxField.get(searchEntry);
						if (editBox != null) {
							editBox.visible = false;
							editBox.setEditable(false);
						}
					}
				} catch (Exception ignored) {}
			}

			if (screen instanceof ContainerScreen chestScreen) {
				String titleTitle = chestScreen.getTitle().getString();
				if (titleTitle.startsWith(SweeperConfig.INSTANCE.trashCanTitle)) {
					if (expectsPageSwitch) {
						GLFW.glfwSetCursorPos(client.getWindow().getWindow(), savedMouseX, savedMouseY);
						expectsPageSwitch = false;
					}

					int maxPages = Math.min(10, Math.max(1, SweeperConfig.INSTANCE.trashCanCount));
					if (maxPages <= 1) return;

					// Extract the current page by finding the first sequence of digits right after the base title
					int currentPage = 1;
					String suffix = titleTitle.substring(SweeperConfig.INSTANCE.trashCanTitle.length());
					java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d+").matcher(suffix);
					if (matcher.find()) {
						try {
							currentPage = Integer.parseInt(matcher.group());
						} catch (Exception ignored) {}
					}

					final int targetPrev = currentPage - 1;
					final int targetNext = currentPage + 1;

					// Find the base coordinates of the container UI background to adjust button positions properly
					int guiLeft = (chestScreen.width - 176) / 2;
					int guiTop = (chestScreen.height - 222) / 2;
					// The space between the container rows and player inventory in a 6-row chest starts around top + 130.

					int centerX = guiLeft + 176 / 2;
					int baseY = guiTop - 20;

					Button prevButton = Button.builder(Component.literal("<"), btn -> {
						savedMouseX = client.mouseHandler.xpos();
						savedMouseY = client.mouseHandler.ypos();
						expectsPageSwitch = true;
						Minecraft.getInstance().player.connection.sendUnsignedCommand("sweeper open " + targetPrev);
					}).bounds(centerX - 15 - 3 + SweeperConfig.INSTANCE.pageControlButtonX, baseY + SweeperConfig.INSTANCE.pageControlButtonY, 15, 15).build();
					prevButton.active = currentPage > 1;
					Screens.getButtons(screen).add(prevButton);

					Button nextButton = Button.builder(Component.literal(">"), btn -> {
						savedMouseX = client.mouseHandler.xpos();
						savedMouseY = client.mouseHandler.ypos();
						expectsPageSwitch = true;
						Minecraft.getInstance().player.connection.sendUnsignedCommand("sweeper open " + targetNext);
					}).bounds(centerX + 3 + SweeperConfig.INSTANCE.pageControlButtonX, baseY + SweeperConfig.INSTANCE.pageControlButtonY, 15, 15).build();
					nextButton.active = currentPage < maxPages;
					Screens.getButtons(screen).add(nextButton);
				}
			}
		});
	}
	// 构建配置UI界面
	public static Screen buildConfigScreen(Screen parent) {
		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.translatable("text.sweeper_maid.config.title"));

		builder.setSavingRunnable(() -> {
			FriendlyByteBuf buf = PacketByteBufs.create();
			buf.writeUtf(SweeperConfig.serialize());
			ClientPlayNetworking.send(SweeperCan.SAVE_CONFIG_PACKET, buf);
		});

		SweeperConfig defaults = new SweeperConfig();

		ConfigCategory general = builder.getOrCreateCategory(Component.translatable("text.sweeper_maid.category.general"));
		ConfigEntryBuilder entryBuilder = builder.entryBuilder();

		general.addEntry(entryBuilder.startIntField(Component.translatable("text.sweeper_maid.option.interval"), SweeperConfig.INSTANCE.intervalTicks)
				.setDefaultValue(defaults.intervalTicks).setSaveConsumer(newValue -> SweeperConfig.INSTANCE.intervalTicks = newValue).build());

		general.addEntry(entryBuilder.startIntField(Component.translatable("text.sweeper_maid.option.maxpages"), SweeperConfig.INSTANCE.trashCanCount)
				.setDefaultValue(defaults.trashCanCount).setMin(1).setMax(10).setSaveConsumer(newValue -> SweeperConfig.INSTANCE.trashCanCount = newValue).build());

		general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.sweeper_maid.option.allow_put"), SweeperConfig.INSTANCE.allowPutItemsInTrashCan)
				.setDefaultValue(defaults.allowPutItemsInTrashCan).setSaveConsumer(newValue -> SweeperConfig.INSTANCE.allowPutItemsInTrashCan = newValue).build());

		ConfigCategory messages = builder.getOrCreateCategory(Component.translatable("text.sweeper_maid.category.messages"));

		messages.addEntry(entryBuilder.startStrField(Component.translatable("text.sweeper_maid.option.msg60"), SweeperConfig.INSTANCE.message60s)
				.setDefaultValue(defaults.message60s).setSaveConsumer(newValue -> SweeperConfig.INSTANCE.message60s = newValue).build());

		messages.addEntry(entryBuilder.startStrField(Component.translatable("text.sweeper_maid.option.msg30"), SweeperConfig.INSTANCE.message30s)
				.setDefaultValue(defaults.message30s).setSaveConsumer(newValue -> SweeperConfig.INSTANCE.message30s = newValue).build());

		messages.addEntry(entryBuilder.startStrField(Component.translatable("text.sweeper_maid.option.msgcd"), SweeperConfig.INSTANCE.messageCountdown)
				.setDefaultValue(defaults.messageCountdown).setSaveConsumer(newValue -> SweeperConfig.INSTANCE.messageCountdown = newValue).build());

		messages.addEntry(entryBuilder.startStrField(Component.translatable("text.sweeper_maid.option.msgclr"), SweeperConfig.INSTANCE.messageCleared)
				.setDefaultValue(defaults.messageCleared).setSaveConsumer(newValue -> SweeperConfig.INSTANCE.messageCleared = newValue).build());

		messages.addEntry(entryBuilder.startStrField(Component.translatable("text.sweeper_maid.option.msgsched"), SweeperConfig.INSTANCE.messageCleanScheduled)
				.setDefaultValue(defaults.messageCleanScheduled).setSaveConsumer(newValue -> SweeperConfig.INSTANCE.messageCleanScheduled = newValue).build());

		ConfigCategory trashCan = builder.getOrCreateCategory(Component.translatable("text.sweeper_maid.category.trashcan"));

		trashCan.addEntry(entryBuilder.startStrField(Component.translatable("text.sweeper_maid.option.title"), SweeperConfig.INSTANCE.trashCanTitle)
				.setDefaultValue(defaults.trashCanTitle).setSaveConsumer(newValue -> SweeperConfig.INSTANCE.trashCanTitle = newValue).build());

		trashCan.addEntry(entryBuilder.startStrField(Component.translatable("text.sweeper_maid.option.suffix"), SweeperConfig.INSTANCE.pageSuffix)
				.setDefaultValue(defaults.pageSuffix).setSaveConsumer(newValue -> SweeperConfig.INSTANCE.pageSuffix = newValue).build());

		trashCan.addEntry(entryBuilder.startIntField(Component.translatable("text.sweeper_maid.option.page_xoffset"), SweeperConfig.INSTANCE.pageControlButtonX)
				.setDefaultValue(defaults.pageControlButtonX).setSaveConsumer(newValue -> SweeperConfig.INSTANCE.pageControlButtonX = newValue).build());

		trashCan.addEntry(entryBuilder.startIntField(Component.translatable("text.sweeper_maid.option.page_yoffset"), SweeperConfig.INSTANCE.pageControlButtonY)
				.setDefaultValue(defaults.pageControlButtonY).setSaveConsumer(newValue -> SweeperConfig.INSTANCE.pageControlButtonY = newValue).build());

		ConfigCategory ui = builder.getOrCreateCategory(Component.translatable("text.sweeper_maid.category.uibutton"));

		ui.addEntry(entryBuilder.startStrField(Component.translatable("text.sweeper_maid.option.showmode"), SweeperConfig.INSTANCE.showInventoryButton)
				.setDefaultValue(defaults.showInventoryButton).setTooltip(Component.translatable("text.sweeper_maid.tooltip.showmode"))
				.setSaveConsumer(newValue -> SweeperConfig.INSTANCE.showInventoryButton = newValue).build());

		ui.addEntry(entryBuilder.startIntField(Component.translatable("text.sweeper_maid.option.xoffset"), SweeperConfig.INSTANCE.inventoryButtonX)
				.setDefaultValue(defaults.inventoryButtonX).setSaveConsumer(newValue -> SweeperConfig.INSTANCE.inventoryButtonX = newValue).build());

		ui.addEntry(entryBuilder.startIntField(Component.translatable("text.sweeper_maid.option.yoffset"), SweeperConfig.INSTANCE.inventoryButtonY)
				.setDefaultValue(defaults.inventoryButtonY).setSaveConsumer(newValue -> SweeperConfig.INSTANCE.inventoryButtonY = newValue).build());

		return builder.build();
	}
}















