package io.siggi.economy.commands;

import io.siggi.economy.EcoHold;
import io.siggi.economy.EcoUser;
import io.siggi.economy.Names;
import io.siggi.economy.SiggiEconomy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public class CommandBal implements CommandExecutor, TabExecutor {

	private final SiggiEconomy plugin;

	public CommandBal(SiggiEconomy plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		UUID me = null;
		if (sender instanceof Player) {
			me = ((Player) sender).getUniqueId();
		}
		UUID who = me;
		if (args.length >= 1) {
			String whoStr = args[0];
			who = Names.get().getUUID(whoStr);
			if (who == null) {
				sender.sendMessage(ChatColor.RED + "Usage: Unknown user " + whoStr);
				return true;
			}
		} else if (who == null) {
			sender.sendMessage(ChatColor.GOLD + "Usage: /bal [user]");
			return true;
		}
		String username = Names.get().getName(who);
		EcoUser user = SiggiEconomy.getUser(who);
		if (me == null || who.equals(me)) {
			double balance = user.getBalance();
			double availableBalance = user.getAvailableBalance();
			sender.sendMessage(ChatColor.GOLD + "Your balance: " + ChatColor.YELLOW + SiggiEconomy.moneyToString(balance));
			if (balance != availableBalance) {
				sender.sendMessage(ChatColor.GOLD + "Available balance: " + ChatColor.YELLOW + SiggiEconomy.moneyToString(availableBalance));
			}
			List<EcoHold> holds = user.getHolds();
			if (!holds.isEmpty()) {
				sender.sendMessage(ChatColor.GOLD + "Funds placed on hold:");
				for (EcoHold hold : holds) {
					sender.sendMessage(ChatColor.YELLOW + SiggiEconomy.moneyToString(hold.getAmount() * hold.getQuantity()) + ChatColor.GOLD + " - " + hold.getInfo());
				}
			}
		} else {
			sender.sendMessage(ChatColor.YELLOW + username + ChatColor.GOLD + "'s balance: " + ChatColor.YELLOW + SiggiEconomy.moneyToString(user.getBalance()) + ChatColor.GOLD + ".");
		}
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> suggestions = new ArrayList<>();
		if (args.length == 1) {
			Names.get().autofill(alias, suggestions);
		}
		return suggestions;
	}

}
