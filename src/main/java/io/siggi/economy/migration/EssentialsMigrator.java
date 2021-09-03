package io.siggi.economy.migration;

import io.siggi.economy.EcoUser;
import io.siggi.economy.SiggiEconomy;
import io.siggi.economy.util.Util;
import java.io.File;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;

public class EssentialsMigrator {

	public void doMigration() {
		File userDataDir = new File(SiggiEconomy.getInstance().getDataFolder(), "essentials_migrationdata");
		for (File userFile : userDataDir.listFiles()) {
			try {
				String fileName = userFile.getName();
				if (fileName.endsWith(".yml")) {
					String uuidStr = fileName.substring(0, fileName.length() - 4);
					UUID uuid = Util.uuidFromString(uuidStr);
					YamlConfiguration conf = YamlConfiguration.loadConfiguration(userFile);
					String eLastAccountName = conf.getString("lastAccountName");
					String moneyStr = conf.getString("money");
					if (moneyStr == null) {
						continue;
					}
					double money = Double.parseDouble(moneyStr);
					EcoUser user = SiggiEconomy.getUser(uuid);
					if (user.getBalance() == 0.0 && money != 0.0) {
						System.out.println("Migrating " + uuid + " / " + eLastAccountName + " / " + money);
						user.performTransaction(money, 1, "Migration from Essentials Economy", true);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
