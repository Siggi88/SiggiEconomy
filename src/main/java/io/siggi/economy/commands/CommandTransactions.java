package io.siggi.economy.commands;

import io.siggi.economy.EcoTransactionLog;
import io.siggi.economy.SiggiEconomy;
import io.siggi.economy.util.Util;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

public class CommandTransactions implements CommandExecutor, TabExecutor {

	private final SiggiEconomy plugin;

	public CommandTransactions(SiggiEconomy plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
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
		
		return suggestions;
	}
	
	private void sendLogs(CommandSender sender, List<EcoTransactionLog> logs) {
		for (EcoTransactionLog log : logs) {
			String amountStr = Util.moneyToString(log.getAmount());
			long quantity = log.getQuantity();
			if (quantity != 1) {
				amountStr = "$" + amountStr + " x" + quantity + " ($" + Util.moneyToString(log.getTotalAmount()) + ")";
			}
			sender.sendMessage(ChatColor.GREEN + "" + log.getTime() + ", amount: " + amountStr + ", new bal: $" + Util.moneyToString(log.getNewBalance()) + ", info: " + log.getInfo());
		}
	}

}
