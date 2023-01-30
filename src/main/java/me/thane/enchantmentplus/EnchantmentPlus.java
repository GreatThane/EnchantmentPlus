package me.thane.enchantmentplus;

import me.thane.enchantmentplus.inventories.ItemSelectScreen;
import me.thane.enchantmentplus.inventories.UpgradeSelectScreen;
import org.bukkit.plugin.java.JavaPlugin;

public final class EnchantmentPlus extends JavaPlugin {

    public static EnchantmentPlus INSTANCE;

    @Override
    public void onLoad() {
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(new ItemSelectScreen.Events(), this);
        getServer().getPluginManager().registerEvents(new UpgradeSelectScreen.Events(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
