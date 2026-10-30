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
        configuredProvider = plugin.getConfig().getString("economy.provider", "VAULT")
                .trim().toUpperCase(Locale.ROOT);
        economy = null;

        if (!configuredProvider.equals("VAULT")
                && !configuredProvider.equals("COINSENGINE")
                && !configuredProvider.equals("EXCELLENTECONOMY")) {
            plugin.getLogger().severe("Unknown economy.provider '" + configuredProvider
                    + "'. Choose VAULT, COINSENGINE, or EXCELLENTECONOMY.");
            return;
        }

        if (configuredProvider.equals("COINSENGINE")
                && !Bukkit.getPluginManager().isPluginEnabled("CoinsEngine")) {
            plugin.getLogger().severe("CoinsEngine is selected but CoinsEngine is not enabled.");
            return;
        }
        if (configuredProvider.equals("EXCELLENTECONOMY")
                && !Bukkit.getPluginManager().isPluginEnabled("ExcellentEconomy")) {
            plugin.getLogger().severe("ExcellentEconomy is selected but ExcellentEconomy is not enabled.");
            return;
        }

        connectVault();
    }

    private boolean connectVault() {
        RegisteredServiceProvider<Economy> registration =
                Bukkit.getServicesManager().getRegistration(Economy.class);
        economy = registration == null ? null : registration.getProvider();
        if (economy == null) {
            plugin.getLogger().severe("No Vault Economy service is registered. Install an economy plugin "
                    + "and enable its Vault integration. Mob bounties cannot be paid until this is fixed.");
            return false;
        }
        plugin.getLogger().info("Economy connected through Vault: " + economy.getName()
                + " (configured provider: " + configuredProvider + ").");
        return true;
    }

    public boolean isAvailable() {
        return economy != null || connectVault();
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (player == null || amount <= 0) return false;
        if (economy == null && !connectVault()) return false;

        try {
            return economy.depositPlayer(player, amount).transactionSuccess();
        } catch (RuntimeException exception) {
            plugin.getLogger().severe("Bounty deposit failed: " + exception.getMessage());
            return false;
        }
    }

    public String getConfiguredProvider() {
        return configuredProvider;
    }
}