package com.flipmobs;

import com.flipmobs.economy.EconomyHook;
import com.flipmobs.listener.MobBountyListener;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class FlipMobs extends JavaPlugin {
    private EconomyHook economyHook;
    private MobBountyListener bountyListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        economyHook = new EconomyHook(this);
        bountyListener = new MobBountyListener(this, economyHook);
        getServer().getPluginManager().registerEvents(bountyListener, this);
        bountyListener.initializeLoadedMobs();
        getLogger().info("FlipMobs enabled. Provider: " + getConfig().getString("economy.provider", "VAULT"));
    }

    @Override
    public void onDisable() {
        if (bountyListener != null) bountyListener.shutdown();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("flipmobs")) return false;
        if (!sender.hasPermission("flipmobs.admin")) {
            sender.sendMessage(color(getConfig().getString("messages.no-permission", "&cNo permission.")));
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            economyHook.reload();
            bountyListener.refreshLoadedMobLabels();
            sender.sendMessage(color(getConfig().getString("messages.prefix", "") +
                    getConfig().getString("messages.reloaded", "&aReloaded.")));
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("info")) {
            String message = getConfig().getString("messages.info", "&7FlipMobs %version% | %provider%");
            message = message.replace("%version%", getDescription().getVersion())
                    .replace("%provider%", getConfig().getString("economy.provider", "VAULT"));
            sender.sendMessage(color(getConfig().getString("messages.prefix", "") + message));
            return true;
        }
        sender.sendMessage(color(getConfig().getString("messages.usage", "&cUsage: /flipmobs <reload|info>")));
        return true;
    }

    public static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }
}