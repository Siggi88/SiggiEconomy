package io.siggi.economy;

import io.siggi.economy.util.Util;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.UUID;

public class EventListener implements Listener {
	private final SiggiEconomy plugin;

	public EventListener(SiggiEconomy plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void login(PlayerLoginEvent event) {
		Player player = event.getPlayer();
		String name = player.getName();
		UUID uniqueId = player.getUniqueId();
		UUID prevUuidForThisName = Names.get().getUUID(name);
		if (prevUuidForThisName != null) {
			if (!prevUuidForThisName.equals(uniqueId)) {
				UUID offlineUuid = Names.offlineUuid(name);
				if (offlineUuid.equals(prevUuidForThisName)) {
					plugin.migrateTransactions(offlineUuid, uniqueId);
				}
			}
		}
		Names.get().set(uniqueId, name);
	}
}
