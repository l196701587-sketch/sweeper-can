package te.sweeper_can;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleContainer;

public class GhostMenu extends ChestMenu {
    public GhostMenu(int syncId, Inventory playerInventory, SimpleContainer container) {
        super(MenuType.GENERIC_9x6, syncId, playerInventory, container, 6);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < 54) {
            ItemStack cursorStack = this.getCarried();
            if (cursorStack.isEmpty()) {
                updateBlacklistItems(ItemStack.EMPTY, slotId);
            } else {
                updateBlacklistItems(cursorStack, -1);
            }
            this.broadcastChanges();
            return; // intercept
        }

        // Let players interact with their own inventory
        if (clickType == ClickType.QUICK_MOVE && slotId >= 54) {
            Slot slot = this.slots.get(slotId);
            if (slot != null && slot.hasItem()) {
                updateBlacklistItems(slot.getItem(), -1);
                this.broadcastChanges();
            }
            return;
        }

        super.clicked(slotId, button, clickType, player);
    }

    private void updateBlacklistItems(ItemStack addStack, int removeIndex) {
        java.util.List<ItemStack> currentItems = new java.util.ArrayList<>();
        for (int i = 0; i < 54; i++) {
            if (i == removeIndex) continue;
            ItemStack existing = this.slots.get(i).getItem();
            if (!existing.isEmpty()) {
                currentItems.add(existing.copy());
            }
        }

        if (addStack != null && !addStack.isEmpty()) {
            ItemStack ghost = addStack.copy();
            ghost.setCount(1);
            boolean duplicate = false;
            for (ItemStack item : currentItems) {
                if (ItemStack.isSameItemSameTags(item, ghost)) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate && currentItems.size() < 54) {
                currentItems.add(ghost);
            }
        }

        for (int i = 0; i < 54; i++) {
            if (i < currentItems.size()) {
                this.slots.get(i).set(currentItems.get(i));
            } else {
                this.slots.get(i).set(ItemStack.EMPTY);
            }
        }
    }
}
