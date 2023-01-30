package me.thane.enchantmentplus.inventories;

import me.thane.enchantmentplus.EnchantmentPlus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UpgradeSelectScreen implements InventoryHolder {

    private final Inventory screen;
    private final Player player;
    private final ItemStack targetItem;

    public UpgradeSelectScreen(ItemStack targetItem, Player player) {
        screen = Bukkit.createInventory(this, 9, Component.text("Upgrades for ").append(targetItem.displayName()));
        this.player = player;
        this.targetItem = targetItem;

        List<ItemStack> upgrades = targetItem.getEnchantments().entrySet().stream()
                .filter(UpgradeSelectScreen::isUpgradeableEnchantment)
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue() + 1))
                .map(UpgradeSelectScreen::convertEnchantmentToBook)
                .collect(Collectors.toList());

        if (targetItem.getEnchantments().containsKey(Enchantment.MENDING)
                && targetItem.getEnchantments().containsKey(Enchantment.DURABILITY)
                && targetItem.getEnchantments().get(Enchantment.DURABILITY) >= 3) {

            ItemStack unbreakableBook = new ItemStack(Material.ENCHANTED_BOOK);
            ItemMeta meta = unbreakableBook.getItemMeta();
            meta.setUnbreakable(true);
            unbreakableBook.setItemMeta(meta);
            upgrades.add(unbreakableBook);
        }
        screen.setContents(upgrades.toArray(ItemStack[]::new));
    }

    private static ItemStack convertEnchantmentToBook(Map.Entry<Enchantment, Integer> enchantment) {
        return convertEnchantmentToBook(enchantment.getKey(), enchantment.getValue());
    }

    private static ItemStack convertEnchantmentToBook(Enchantment enchantment, int level) {
        ItemStack upgrade = new ItemStack(Material.ENCHANTED_BOOK);
        upgrade.addUnsafeEnchantment(enchantment, level);
        return upgrade;
    }

    private static final HashSet<Enchantment> DISALLOWED_ENCHANTMENTS = new HashSet<>() {{
        add(Enchantment.BINDING_CURSE);
        add(Enchantment.VANISHING_CURSE);
        add(Enchantment.ARROW_INFINITE);
        add(Enchantment.MENDING);
    }};

    static boolean isUpgradeableEnchantment(Map.Entry<Enchantment, Integer> enchantment) {
        return isUpgradeableEnchantment(enchantment.getKey(), enchantment.getValue());
    }

    static boolean isUpgradeableEnchantment(Enchantment enchantment, int level) {
        return !DISALLOWED_ENCHANTMENTS.contains(enchantment);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return screen;
    }

    private static boolean areItemsIdentical(ItemStack i1, ItemStack i2) {
        if (i1 == null || i2 == null) return false;
        if (!i1.equals(i2)) return false;
        if (i1.getItemMeta() == null && i2.getItemMeta() == null) return true;
        if (i1.getItemMeta() == null) return false;
        if (i2.getItemMeta() == null) return false;
        return i1.getItemMeta().getAsString().equals(i2.getItemMeta().getAsString());
    }

    public static class Events implements Listener {

        @EventHandler(ignoreCancelled = true)
        public void onInventoryClick(InventoryClickEvent event) {
            if (event.getClickedInventory() == null) return;
            if (!(event.getClickedInventory().getHolder() instanceof UpgradeSelectScreen screen)) return;
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            if (!event.getWhoClicked().getUniqueId().equals(screen.player.getUniqueId())) return;

            Map.Entry<Enchantment, Integer> enchantment = event.getCurrentItem().getEnchantments().entrySet().stream().findAny().orElse(null);

            ItemStack[] contents = screen.player.getInventory().getContents();
            Integer targetSlot = null;
            for (int i = 0; i < contents.length; i++) {
                if (areItemsIdentical(screen.targetItem, contents[i])) {
                    targetSlot = i;
                    break;
                }
            }
            if (targetSlot == null) return;

            int cost = 1;
            if (!screen.player.getInventory().containsAtLeast(new ItemStack(Material.NETHERITE_INGOT), cost)) {
                screen.player.sendMessage(ChatColor.RED + "You need at least " + cost + " netherite ingot" + (cost != 1 ? "s" : "") + " to upgrade!");
                return;
            }

            ItemStack targetItem = screen.player.getInventory().getItem(targetSlot);
            if (targetItem == null) return;

            while (cost > 0) {
                int slot = screen.player.getInventory().first(Material.NETHERITE_INGOT);
                if (slot < 0) {
                    screen.player.sendMessage(ChatColor.RED + "You cannot afford this upgrade!");
                    return;
                }
                ItemStack item = screen.player.getInventory().getItem(slot);
                if (item == null) return;
                if (item.getAmount() <= cost) {
                    cost -= item.getAmount();
                    screen.player.getInventory().setItem(slot, new ItemStack(Material.AIR));
                } else {
                    cost = 0;
                    item.setAmount(item.getAmount() - cost);
                    screen.player.getInventory().setItem(slot, item);
                }
            }

            if (enchantment != null) {
                targetItem.removeEnchantment(enchantment.getKey());
                targetItem.addUnsafeEnchantment(enchantment.getKey(), enchantment.getValue());
            } else if (event.getCurrentItem().getItemMeta().isUnbreakable()) {
                ItemMeta meta = targetItem.getItemMeta();
                meta.setUnbreakable(true);
                meta.removeEnchant(Enchantment.MENDING);
                meta.removeEnchant(Enchantment.DURABILITY);
                targetItem.setItemMeta(meta);
            } else {
                throw new RuntimeException("Item wasn't enchanted or unbreakable");
            }
            screen.player.getInventory().setItem(targetSlot, targetItem);

            Bukkit.getScheduler().runTaskLater(EnchantmentPlus.INSTANCE, () -> {
                screen.player.closeInventory();
            }, 1);
        }

        @EventHandler(ignoreCancelled = true)
        public void onInventoryMoveItem(InventoryMoveItemEvent event) {
            if (Stream.of(event.getDestination(), event.getSource())
                    .anyMatch(i -> i.getHolder() instanceof UpgradeSelectScreen)) {
                event.setCancelled(true);
            }
        }
    }
}
