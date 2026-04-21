package te.sweeper_can;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class SweeperConfig {
    public String _comment_intervalSeconds = "自动清理的时间间隔 (秒)。";
    public int intervalSeconds = 300;

    public String _comment_commandOpLevel = "执行管理指令所需的OP权限等级 (默认 2)";
    public int commandOpLevel = 2;

    public String _comment_commandEmptyOpLevel = "执行清空垃圾桶指令所需的OP权限等级 (默认 2)";
    public int commandEmptyOpLevel = 2;

    public String _comment_messages = "倒计时和清理结果的通报消息配置";
    public String message60s = "§e将在 60 秒后清理掉落物...";
    public String message30s = "§e将在 30 秒后清理掉落物...";
    public String messageCountdown = "§c将在 %d 秒后清理掉落物...";
    public String messageCleared = "§a清理了 %d 个掉落物。";

    public String _comment_enableCountdownBroadcast = "是否开启倒计时播报功能";
    public boolean enableCountdownBroadcast = true;

    public String _comment_countdownSeconds = "在这个秒数内，每秒在聊天栏进行倒计时播报（前提是开启倒计时播报）";
    public int countdownSeconds = 10;

    public String _comment_enableClearedBroadcast = "清理结束后，是否在聊天栏播报清理了多少掉落物";
    public boolean enableClearedBroadcast = true;

    public String chatMessageCountdown = "§c【提醒】掉落物将在 %d 秒后清理...";
    public String chatMessageCleared = "§a【提示】本次已清理 %d 个掉落物。";

    public String _comment_autoEmpty = "--- 垃圾桶定时清空 ---";
    public boolean enableAutoEmpty = false;
    public int autoEmptyInterval = 1800;
    public boolean enableAutoEmptyCountdown = true;
    public String autoEmptyMsg180s = "§e【提醒】垃圾桶将在 180 秒后自动清空...";
    public String autoEmptyMsg60s = "§e【提醒】垃圾桶将在 60 秒后自动清空...";
    public String autoEmptyMsg10s = "§c【警告】垃圾桶将在 10 秒后自动清空！";
    public boolean enableAutoEmptyClearedMsg = true;
    public String autoEmptyMsgCleared = "§a【提示】垃圾桶已自动清空。";

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

    public String _comment_clearControlPos = "清除按钮在垃圾桶界面的偏移量";
    public int clearButtonX = 0;
    public int clearButtonY = 0;

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
