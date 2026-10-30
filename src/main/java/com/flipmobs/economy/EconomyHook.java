package com.flipmobs.economy;

import com.flipmobs.FlipMobs;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import java.util.Locale;

public final class EconomyHook {
    private final FlipMobs plugin;
    private Economy economy;
    private String configuredProvider;

    public EconomyHook(FlipMobs plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        configuredProvider = plugin.getConfig().getString("economy.provider", "VAULT").trim().toUpperCase(Locale.ROOT);
        economy = null;
        if (!configuredProvider.equals("VAULT") && !configuredProvider.equals("COINSENGINE")
                && !configuredProvider.equals("EXCELLENTECONOMY")) {
            plugin.getLogger().warning("Unknown economy provider: " + configuredProvider);
            return;
        }
        if (configuredProvider.equals("COINSENGINE") && !Bukkit.getPluginManager().isPluginEnabled("CoinsEngine")) {
            plugin.getLogger().warning("CoinsEngine is selected but is not enabled.");
            return;
        }
        if (configuredProvider.equals("EXCELLENTECONOMY") && !Bukkit.getPluginManager().isPluginEnabled("ExcellentEconomy")) {
            plugin.getLogger().warning("ExcellentEconomy is selected but is not enabled.");
            return;
        }
        RegisteredServiceProvider<Economy> registration = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (registration != null) {
            economy = registration.getProvider();
            plugin.getLogger().info("Connected to " + configuredProvider + " through Vault.");
        } else {
            plugin.getLogger().warning("No Vault economy service found. Enable the Vault hook in your economy plugin.");
        }
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (economy == null || player == null || amount <= 0) return false;
        try {
            return economy.depositPlayer(player, amount).transactionSuccess();
        } catch (RuntimeException exception) {
            plugin.getLogger().severe("Bounty deposit failed: " + exception.getMessage());
            return false;
        }
    }
}