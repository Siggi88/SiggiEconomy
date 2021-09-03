package io.siggi.economy.commands;

import io.siggi.economy.EcoTransactionLog;
import io.siggi.economy.EcoTransactionResult;
import io.siggi.economy.EcoUser;
import io.siggi.economy.Names;
import io.siggi.economy.SiggiEconomy;
import io.siggi.economy.migration.EssentialsMigrator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public class CommandEco implements CommandExecutor, TabExecutor {

	private final SiggiEconomy plugin;
	private boolean shouldShowEssentialsMigrationAutoCompletion;

	public CommandEco(SiggiEconomy plugin, boolean shouldShowEssentialsMigrationAutoCompletion) {
		this.plugin = plugin;
		this.shouldShowEssentialsMigrationAutoCompletion = shouldShowEssentialsMigrationAutoCompletion;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		UUID me = null;
		if (sender instanceof Player) {
			me = ((Player) sender).getUniqueId();
		}
		switch (args[0]) {
			case "adjust": {
				if (args.length < 3) {
					sender.sendMessage(ChatColor.RED + "Usage: /eco adjust [playername] [amount] [optional description]");
					break;
				}
				String whoStr = args[1];
				UUID who = Names.get().getUUID(whoStr);
				if (who == null) {
					sender.sendMessage(ChatColor.RED + "Usage: /eco adjust [playername] [amount] [optional description]");
					break;
				}
				if (!sender.hasPermission("io.siggi.economy.adjust")) {
					sender.sendMessage(ChatColor.RED + "You don't have permission to do that!");
					break;
				}
				double amount = Double.parseDouble(args[2]);
				String info = "Admin Adjustment";
				if (args.length >= 4) {
					info = args[3];
					for (int i = 4; i < args.length; i++) {
						info += " " + args[i];
					}
				}
				EcoTransactionResult result = SiggiEconomy.getUser(who).performTransaction(amount, info);
				if (result.isSuccessful()) {
					sender.sendMessage(ChatColor.GREEN + "Completed! New balance: " + SiggiEconomy.moneyToString(result.getLog().getNewBalance()));
				} else {
					sender.sendMessage(ChatColor.RED + "Failed: " + result.getResult().getDescription());
				}
			}
			break;
			case "migratefromessentials": {
				if (sender.hasPermission("io.siggi.economy.adjust")) {
					new EssentialsMigrator().doMigration();
				}
				shouldShowEssentialsMigrationAutoCompletion = false;
			}
			break;
		}
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> suggestions = new ArrayList<>();
		Consumer<String> suggest = (suggestion) -> {
			if (suggestion.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
				suggestions.add(suggestion);
			}
		};
		if (args.length == 1) {
			if (sender.hasPermission("io.siggi.economy.adjust")) {
				suggest.accept("adjust");
			}
			if (shouldShowEssentialsMigrationAutoCompletion && sender.hasPermission("io.siggi.economy.adjust")) {
				suggest.accept("migratefromessentials");
			}
		}
		if (args.length == 2) {
			switch (args[0]) {
				case "adjust": {
					Names.get().autofill(args[1], suggestions);
				}
				break;
			}
		}
		return suggestions;
	}
}
