package io.siggi.economy.commands;

import io.siggi.economy.EcoTransactionResult;
import io.siggi.economy.EcoUser;
import io.siggi.economy.Names;
import io.siggi.economy.SiggiEconomy;
import io.siggi.economy.util.Util;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public class CommandPay implements CommandExecutor, TabExecutor {

	private final SiggiEconomy plugin;

	public CommandPay(SiggiEconomy plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "Only players can use this command! :P");
			return true;
		}
		Player p = (Player) sender;
		if (args.length < 2) {
			p.sendMessage(ChatColor.GOLD + "Usage: " + ChatColor.YELLOW + "/pay [recipient] [amount]");
			return true;
		}
		try {
			String myName = Names.get().getName(p.getUniqueId());
			String recipientName = args[0];
			UUID recipientUUID = Names.get().getUUID(recipientName);
			if (recipientUUID == null) {
				p.sendMessage(ChatColor.RED + "Unknown user " + recipientName);
				return true;
			}
			recipientName = Names.get().getName(recipientUUID);
			double amount = Double.parseDouble(args[1]);
			EcoUser myWallet = SiggiEconomy.getUser(p.getUniqueId());
			EcoTransactionResult result = myWallet.withdraw(amount, "Payment to [user:" + recipientUUID.toString() + "]");
			if (!result.isSuccessful()) {
				p.sendMessage(ChatColor.RED + "Failed to pay: " + result.getResult().getDescription());
				return true;
			}
			EcoUser recipientWallet = SiggiEconomy.getUser(recipientUUID);
			EcoTransactionResult depResult = recipientWallet.deposit(amount, "Payment from [user:" + p.getUniqueId().toString() + "]");
			if (!depResult.isSuccessful()) {
				p.sendMessage(ChatColor.RED + "Something went wrong: " + result.getResult().getDescription());
				return true;
			}
			p.sendMessage(ChatColor.GOLD + "Paid " + ChatColor.YELLOW + "$" + Util.moneyToString(amount) + ChatColor.GOLD + " to " + ChatColor.YELLOW + recipientName + ChatColor.GOLD + "!");
			Player recipient = Bukkit.getPlayer(recipientUUID);
			if (recipient != null) {
				recipient.sendMessage(ChatColor.GOLD + "You received " + ChatColor.YELLOW + "$" + Util.moneyToString(amount) + ChatColor.GOLD + " from " + ChatColor.YELLOW + myName + ChatColor.GOLD + "!");
			}
		} catch (Exception e) {
			p.sendMessage(ChatColor.GOLD + "Usage: " + ChatColor.YELLOW + "/pay [recipient] [amount]");
		}
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> suggestions = new ArrayList<>();
		if (args.length == 1) {
			Names.get().autofill(args[0], suggestions);
		}
		return suggestions;
	}

}
