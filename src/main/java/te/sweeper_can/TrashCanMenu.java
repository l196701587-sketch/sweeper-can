package te.sweeper_can;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Container;

public class TrashCanMenu extends ChestMenu {
    public TrashCanMenu(int syncId, Inventory playerInventory, Container container) {
        super(MenuType.GENERIC_9x6, syncId, playerInventory, container, 6);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (!SweeperConfig.INSTANCE.allowPutItemsInTrashCan) {
            // Prevent taking items from cursor and putting into the trash can (0-53 are chest slots)
            if (slotId >= 0 && slotId < 54) {
                // Intercept put actions
                if (!this.getCarried().isEmpty()) {
                    // Just return to cancel the action and not place item
                    return;
                }
            }

            // Prevent shift-clicking from player inventory into trash can
            if (clickType == ClickType.QUICK_MOVE && slotId >= 54) {
                return;
            }
        }

        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!SweeperConfig.INSTANCE.allowPutItemsInTrashCan) {
            if (index >= 54) {
                return ItemStack.EMPTY;
            }
        }
        return super.quickMoveStack(player, index);
    }
}
