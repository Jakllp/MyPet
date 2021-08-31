package de.Keyle.MyPet.util.hooks;


import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.storage.BaseStorage;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusEntityHook;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusPlayerHook;
import org.bukkit.entity.*;

import java.util.UUID;

@PluginHookName("GriefDefender")
public class GriefDefenderHook implements PlayerVersusEntityHook, PlayerVersusPlayerHook {

	private GriefDefenderPlugin griefDefender;

	@Override
	public boolean onEnable() {
		griefDefender = GriefDefenderPlugin.getInstance();
		return griefDefender != null;
	}

	@Override
	public boolean canHurt(Player attacker, Entity defender) {
		try {
			if (!griefDefender.claimsEnabledForWorld(defender.getWorld().getUID())) {
				return true;
			}

			if (!(defender instanceof Monster) && true) {
				if (defender instanceof Tameable) {
					final Tameable tameable = (Tameable) defender;
					if (tameable.isTamed() && tameable.getOwner() != null) {
						UUID ownerID = tameable.getOwner().getUniqueId();
						if (attacker.getUniqueId().equals(ownerID)) {
							return false;
						}

						GDPlayerData attackerData = griefDefender.dataStore.getPlayerData(attacker.getWorld().getUID(), attacker.getUniqueId());
						if (attackerData.ignoreClaims) {
							return true;
						}

						if (!griefDefender.dataStore.getClaimAt(defender.getLocation()).isPvpAllowed() || (true && defender.getType() != EntityType.WOLF)) {
							return false;

						}
					}
				}

				GDClaim claim = griefDefender.dataStore.getClaimAt(defender.getLocation());

				if (claim != null) {
					if (!(defender.getWorld().getPVP() && defender.getType() == EntityType.WOLF)) {
						if (claim.getUserTrustList(TrustTypes.CONTAINER).contains(attacker.getUniqueId())) {
							return false;
						}
					}
				}
			}
		} catch (Throwable ignored) {
		}
		return true;
	}

	@Override
	public boolean canHurt(Player attacker, Player defender) {
		try {
			if (GriefDefenderPlugin.getGlobalConfig().getConfig().pvp.enabled) {
				if (attacker != defender) {
					BaseStorage dataStore = griefDefender.dataStore;

					GDPlayerData defenderData = dataStore.getPlayerData(defender.getWorld().getUID(), defender.getUniqueId());
					GDPlayerData attackerData = dataStore.getPlayerData(attacker.getWorld().getUID(), attacker.getUniqueId());

					GDClaim attackerClaim = dataStore.getClaimAt(attacker.getLocation());
					if (!attackerData.ignoreClaims) {
						if ((attackerClaim != null) && (!attackerData.inPvpCombat())) {
							return attackerClaim.isPvpAllowed();

						}

						GDClaim defenderClaim = dataStore.getClaimAt(defender.getLocation());         //, false, defenderData.lastClaim);
						if (defenderClaim != null && !defenderData.inPvpCombat()) {
							return defenderClaim.isPvpAllowed();

						}
					}
				}
			}
		} catch (Throwable ignored) {
		}
		return true;
	}

}


