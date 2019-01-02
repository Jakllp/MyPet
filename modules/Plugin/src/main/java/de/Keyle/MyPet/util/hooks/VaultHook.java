/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.util.hooks;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.leashing.LeashFlag;
import de.Keyle.MyPet.api.entity.leashing.LeashFlagName;
import de.Keyle.MyPet.api.entity.leashing.LeashFlagSetting;
import de.Keyle.MyPet.api.entity.leashing.LeashFlagSettings;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.EconomyHook;
import de.Keyle.MyPet.api.util.hooks.types.PermissionGroupHook;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;

@PluginHookName("Vault")
public class VaultHook implements EconomyHook, PermissionGroupHook {

    private Economy economy = null;
    private Permission permission = null;

    @Override
    public boolean onEnable() {
        boolean enabled = false;
        if (Configuration.Hooks.USE_ECONOMY) {
            RegisteredServiceProvider<Economy> economyProvider = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
            if (economyProvider != null) {
                economy = economyProvider.getProvider();
                enabled = true;
            }
        }
        RegisteredServiceProvider<Permission> permissionProvider = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
        if (permissionProvider != null) {
            permission = permissionProvider.getProvider();
            enabled = true;
            MyPetApi.getLeashFlagManager().registerLeashFlag(new PermissionGroupFlag());
        }
        return enabled;
    }

    @Override
    public void onDisable() {
        MyPetApi.getLeashFlagManager().removeFlag("PermissionGroup");
        economy = null;
        permission = null;
    }

    @Override
    public String getActivationMessage() {
        try {
            return " (Economy: " + economy.getName() + ")";
        } catch (UnsupportedOperationException e) {
            return " (Economy: " + economy.getClass().getName() + ")";
        }
    }

    @Override
    public boolean canPay(MyPetPlayer petOwner, double costs) {
        return canPay(petOwner.getPlayer(), costs);
    }

    @Override
    public boolean canPay(UUID playerUUID, double costs) {
        return canPay(Bukkit.getOfflinePlayer(playerUUID), costs);
    }

    @Override
    public boolean canPay(OfflinePlayer player, double costs) {
        try {
            return economy.has(player, costs);
        } catch (Throwable ignored) {
        }
        return false;
    }

    @Override
    public boolean transfer(MyPetPlayer from, MyPetPlayer to, double costs) {
        return transfer(from.getPlayer(), to.getPlayer(), costs);
    }

    @Override
    public boolean transfer(UUID from, UUID to, double costs) {
        return transfer(Bukkit.getOfflinePlayer(from), Bukkit.getOfflinePlayer(to), costs);
    }

    @Override
    public boolean transfer(OfflinePlayer from, OfflinePlayer to, double costs) {
        if (economy.has(from, costs)) {
            try {
                return economy.withdrawPlayer(from, costs).transactionSuccess() && economy.depositPlayer(to, costs).transactionSuccess();
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    @Override
    public boolean pay(MyPetPlayer petOwner, double costs) {
        return pay(petOwner.getPlayer(), costs);
    }

    @Override
    public boolean pay(UUID playerUUID, double costs) {
        return pay(Bukkit.getOfflinePlayer(playerUUID), costs);
    }

    @Override
    public boolean pay(OfflinePlayer player, double costs) {
        if (economy.has(player, costs)) {
            try {
                return economy.withdrawPlayer(player, costs).transactionSuccess();
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    @Override
    public String currencyNameSingular() {
        try {
            return economy.currencyNameSingular();
        } catch (Throwable ignored) {
        }
        return "";
    }

    @Override
    public String format(double amount) {
        try {
            return economy.format(amount);
        } catch (Throwable ignored) {
        }
        return "" + amount;
    }

    public Economy getEconomy() {
        return economy;
    }

    @Override
    public boolean isInGroup(Player player, String group) {
        if (permission.hasGroupSupport()) {
            return permission.playerInGroup(player.getWorld().getName(), player, group);
        }
        return false;
    }

    @Override
    public boolean isInGroup(Player player, String group, String world) {
        if (permission.hasGroupSupport()) {
            return permission.playerInGroup(world, player, group);
        }
        return false;
    }

    @LeashFlagName("PermissionGroup")
    public class PermissionGroupFlag implements LeashFlag {

        @Override
        public boolean check(Player player, LivingEntity entity, double damage, LeashFlagSettings settings) {
            String world = null;
            String group = null;
            for (LeashFlagSetting setting : settings.all()) {
                if (setting.getKey().equalsIgnoreCase("world")) {
                    world = setting.getValue();
                } else {
                    group = setting.getValue();
                }
            }
            if (group != null) {
                if (world != null) {
                    return isInGroup(player, group, world);
                }
                return isInGroup(player, group);
            }
            return true;
        }
    }
}