package te.sweeper_can;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class SweeperConfig {
    public String _comment_intervalTicks = "自动清理的时间间隔 (ticks) 20 ticks = 1秒。";
    public int intervalTicks = 6000;

    public String _comment_commandOpLevel = "执行管理指令所需的OP权限等级 (默认 2)";
    public int commandOpLevel = 2;

    public String _comment_messages = "倒计时和清理结果的通报消息配置";
    public String message60s = "§e将在 60 秒后清理掉落物...";
    public String message30s = "§e将在 30 秒后清理掉落物...";
    public String messageCountdown = "§c将在 %d 秒后清理掉落物...";
    public String messageCleared = "§a清理了 %d 个掉落物。";

    public String _comment_messageCleanScheduled = "使用/sweeper clean指令时显示的提示消息，%d 将被替换为设定的时间（秒）";
    public String messageCleanScheduled = "§e将在 %d 秒后清理掉落物";

    public String _comment_trashCan = "--- 垃圾桶界面的标题和翻页相关配置名称 ---";

    public String _comment_trashCanTitle = "垃圾桶界面的标题";
    public String trashCanTitle = "垃圾桶";

    public String _comment_trashCanCount = "垃圾桶的数量";
    public int trashCanCount = 1;

    public String _comment_pageSuffix = "垃圾桶界面标题的页数后缀";
    public String pageSuffix = " (第 %d 页)";

    public String _comment_messageAmountSet = "使用/sweeper amount指令设定垃圾桶数量后，所显示的提示消息，%d 将被替换为设定的数量";
    public String messageAmountSet = "§a已设置垃圾桶的数量为 %d 个。";

    public String _comment_showInventoryButton = "是否在物品栏显示打开垃圾桶的按钮：false(不显示)、survival(仅生存)、creative(仅创造)、all(全部显示)";
    public String showInventoryButton = "all";

    public String _comment_buttonPos = "该快捷按钮在物品栏的偏移量（基础位置在屏幕水平居中，垂直靠上处）";
    public int inventoryButtonX = 0;
    public int inventoryButtonY = 0;

    public String _comment_pageControlPos = "翻页按钮在垃圾桶界面的偏移量（基础位置在界面水平居中，垂直靠上处）";
    public int pageControlButtonX = 0;
    public int pageControlButtonY = 0;

    public String _comment_allowPutItemsInTrashCan = "是否能让玩家从背包里把物品放入垃圾桶里";
    public boolean allowPutItemsInTrashCan = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "sweeper_can.json");
    public static SweeperConfig INSTANCE = new SweeperConfig();

	public static String serialize() {
		return GSON.toJson(INSTANCE);
	}

	public static void deserialize(String json) {
		INSTANCE = GSON.fromJson(json, SweeperConfig.class);
	}

    public static void loadConfig() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                INSTANCE = GSON.fromJson(reader, SweeperConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            saveConfig();
        }
    }

    public static void saveConfig() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}