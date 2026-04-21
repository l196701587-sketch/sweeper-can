package te.sweeper_can;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

public class SweeperState extends SavedData {
    public final SimpleContainer[] inventories = new SimpleContainer[10];
    public final SimpleContainer blacklist = new SimpleContainer(54);

    public SweeperState() {
        for (int i = 0; i < 10; i++) {
            inventories[i] = new SimpleContainer(54);
            inventories[i].addListener(sender -> this.setDirty());
        }
        blacklist.addListener(sender -> this.setDirty());
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        for (int p = 0; p < 10; p++) {
            ListTag list = new ListTag();
            SimpleContainer inventory = inventories[p];
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty()) {
                    CompoundTag itemTag = new CompoundTag();
                    itemTag.putByte("Slot", (byte) i);
                    stack.save(itemTag);
                    list.add(itemTag);
                }
            }
            tag.put("Items_Page_" + p, list);
        }

        ListTag blacklistTag = new ListTag();
        for (int i = 0; i < blacklist.getContainerSize(); i++) {
            ItemStack stack = blacklist.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                stack.save(itemTag);
                blacklistTag.add(itemTag);
            }
        }
        tag.put("Blacklist", blacklistTag);

        return tag;
    }

    public static SweeperState load(CompoundTag tag) {
        SweeperState state = new SweeperState();
        for (int p = 0; p < 10; p++) {
            if (tag.contains("Items_Page_" + p, Tag.TAG_LIST)) {
                ListTag list = tag.getList("Items_Page_" + p, Tag.TAG_COMPOUND);
                SimpleContainer inventory = state.inventories[p];
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag itemTag = list.getCompound(i);
                    int slot = itemTag.getByte("Slot") & 255;
                    if (slot >= 0 && slot < inventory.getContainerSize()) {
                        inventory.setItem(slot, ItemStack.of(itemTag));
                    }
                }
            } else if (p == 0 && tag.contains("Items", Tag.TAG_LIST)) { // backwards compatibility for old save
                ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
                SimpleContainer inventory = state.inventories[0];
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag itemTag = list.getCompound(i);
                    int slot = itemTag.getByte("Slot") & 255;
                    if (slot >= 0 && slot < inventory.getContainerSize()) {
                        inventory.setItem(slot, ItemStack.of(itemTag));
                    }
                }
            }
        }

        if (tag.contains("Blacklist", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Blacklist", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag itemTag = list.getCompound(i);
                int slot = itemTag.getByte("Slot") & 255;
                if (slot >= 0 && slot < state.blacklist.getContainerSize()) {
                    state.blacklist.setItem(slot, ItemStack.of(itemTag));
                }
            }
        }

        return state;
    }

    public static SweeperState getServerState(MinecraftServer server) {
        DimensionDataStorage storage = server.getLevel(Level.OVERWORLD).getDataStorage();
        java.util.function.Function<CompoundTag, SweeperState> factory = SweeperState::load;
        java.util.function.Supplier<SweeperState> supplier = SweeperState::new;
        return storage.computeIfAbsent(
                factory,
                supplier,
                "sweeper_can"
        );
    }
}
