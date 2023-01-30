package me.thane.enchantmentplus.inventories;

import com.google.gson.Gson;
import me.thane.enchantmentplus.EnchantmentPlus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Stream;

public class ItemSelectScreen implements InventoryHolder {

    private final Inventory screen;

    public ItemSelectScreen() {
        screen = Bukkit.createInventory(this, 9, Component.text("Select an item to upgrade"));
        ItemStack[] items = new ItemStack[9];
        ItemStack helpText = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = helpText.getItemMeta();
        assert meta != null;
        meta.displayName(Component.text("Select an item from your inventory to upgrade."));
        helpText.setItemMeta(meta);
        Arrays.fill(items, helpText);
        screen.setStorageContents(items);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return screen;
    }

    private static boolean isUpgradeableItem(ItemStack stack) {
        return stack.getEnchantments().entrySet().stream()
                .anyMatch(e -> UpgradeSelectScreen.isUpgradeableEnchantment(e.getKey(), e.getValue()));
    }

    public static boolean isObelisk(Block block) {
        return (block.getType().equals(Material.SHROOMLIGHT) && block.getRelative(BlockFace.DOWN).getType().equals(Material.ANCIENT_DEBRIS))
                || (block.getType().equals(Material.ANCIENT_DEBRIS) && block.getRelative(BlockFace.UP).getType().equals(Material.SHROOMLIGHT));
    }

    public static class Events implements Listener {

        @EventHandler(ignoreCancelled = true)
        public void onPlayerInteract(PlayerInteractEvent event) {
            if (event.getHand() == EquipmentSlot.OFF_HAND) return;
            if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                if (event.getClickedBlock() == null || !isObelisk(event.getClickedBlock())) {
                    return;
                }
                Player accessor = event.getPlayer();
                if (!accessor.getInventory().containsAtLeast(new ItemStack(Material.NETHERITE_INGOT), 1)) {
                    accessor.sendMessage(ChatColor.RED + "You need at least 1 netherite ingot to upgrade an enchantment!");
                    return;
                }

                accessor.openInventory(new ItemSelectScreen().getInventory());
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onInventoryClick(InventoryClickEvent event) {
            if (!(event.getView().getTopInventory().getHolder() instanceof ItemSelectScreen)) return;
            event.setCancelled(true);

            ItemStack targetItem = event.getCurrentItem();
            if (targetItem == null || targetItem.getType().isAir()) {
                return;
            }
            if (!(event.getWhoClicked() instanceof Player accessor)) {
                return;
            }
            if (!isUpgradeableItem(targetItem)) {
                accessor.sendMessage(targetItem.displayName().color(NamedTextColor.RED)
                        .append(Component.text(" is not upgradable.", NamedTextColor.RED))
                        .asComponent());
                return;
            }

            Bukkit.getScheduler().runTaskLater(EnchantmentPlus.INSTANCE, () -> {
                accessor.closeInventory();
                accessor.openInventory(new UpgradeSelectScreen(targetItem, accessor).getInventory());
            }, 1);
        }

        @EventHandler(ignoreCancelled = true)
        public void onInventoryMoveItem(InventoryMoveItemEvent event) {
            if (Stream.concat(Stream.of(event.getDestination(), event.getInitiator(), event.getSource()),
                            event.getSource().getViewers().stream().map(v -> v.getOpenInventory().getTopInventory()))
                    .anyMatch(i -> i.getHolder() instanceof ItemSelectScreen)) {
                event.setCancelled(true);
            }
        }
    }
}
