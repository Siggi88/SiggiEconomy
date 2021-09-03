package io.siggi.economy.commands;

import io.siggi.economy.EcoTransactionLog;
import io.siggi.economy.Names;
import io.siggi.economy.SiggiEconomy;

import java.util.*;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import static io.siggi.economy.util.Util.timeToString;

public class CommandTransactions implements CommandExecutor, TabExecutor {

	private final SiggiEconomy plugin;

	public CommandTransactions(SiggiEconomy plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		UUID uuid;
		if (args.length == 0) {
			if (sender instanceof Player) {
				uuid = ((Player) sender).getUniqueId();
			} else {
				sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <playername>");
				return true;
			}
		} else {
			uuid = Names.get().getUUID(args[0]);
			if (uuid == null) {
				sender.sendMessage(ChatColor.RED + "Unknown user " + args[0] + ".");
				return true;
			}
		}
		boolean reverse = true;
		long time = System.currentTimeMillis();
		if (args.length > 1) {
			reverse = false;
			String str = args[1];
			if (str.startsWith("r")) {
				str = str.substring(1);
				reverse = true;
			}
			time = Long.parseLong(str);
		}
		ListIterator<EcoTransactionLog> logIterator = SiggiEconomy.getUser(uuid).getTransactionLogs(time);
		List<EcoTransactionLog> logs = new LinkedList<>();
		while (logs.size() < 10) {
			if (reverse) {
				if (!logIterator.hasPrevious()) {
					break;
				}
				logs.add(logIterator.previous());
			} else {
				if (!logIterator.hasNext()) {
					break;
				}
				logs.add(0, logIterator.next());
			}
		}
		String name = Names.get().getName(uuid);
		sender.sendMessage(ChatColor.GOLD + "Transactions for " + name);
		if (logs.isEmpty()) {
			sender.sendMessage(ChatColor.RED + "No transactions found.");
			return true;
		}
		for (EcoTransactionLog log : logs) {
			sendLog(sender, log);
		}
		if (SiggiEconomy.isBungeeChatApiAvailable()) {
			EcoTransactionLog first = logs.get(logs.size() - 1);
			EcoTransactionLog last = logs.get(0);

			TextComponent chatLine = new TextComponent("");

			TextComponent navigateText = new TextComponent("Navigate: ");
			navigateText.setColor(net.md_5.bungee.api.ChatColor.GOLD);
			chatLine.addExtra(navigateText);

			TextComponent olderTransactions = new TextComponent(" [Older Transactions]");
			olderTransactions.setColor(net.md_5.bungee.api.ChatColor.AQUA);
			olderTransactions.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/transactions " + name + " r" + first.getTime()));
			chatLine.addExtra(olderTransactions);

			TextComponent newerTransactions = new TextComponent(" [Newer Transactions]");
			newerTransactions.setColor(net.md_5.bungee.api.ChatColor.AQUA);
			newerTransactions.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/transactions " + name + " " + (last.getTime() + 1L)));
			chatLine.addExtra(newerTransactions);

			sender.spigot().sendMessage(chatLine);
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

	private void sendLog(CommandSender sender, EcoTransactionLog log) {
		long now = System.currentTimeMillis();
		if (SiggiEconomy.isBungeeChatApiAvailable()) {
			TextComponent rootComponent = new TextComponent("");

			String time = timeToString(now - log.getTime());
			TextComponent timeComponent = new TextComponent(time + " ");
			timeComponent.setColor(net.md_5.bungee.api.ChatColor.GOLD);

			double total = log.getTotalAmount();
			String totalString = SiggiEconomy.moneyToString(total);
			TextComponent amountComponent = new TextComponent(totalString + " ");
			if (total < 0) {
				amountComponent.setColor(net.md_5.bungee.api.ChatColor.RED);
			} else {
				amountComponent.setColor(net.md_5.bungee.api.ChatColor.GREEN);
			}

			TextComponent infoComponent = new TextComponent(log.getInfo());
			infoComponent.setColor(net.md_5.bungee.api.ChatColor.GOLD);

			rootComponent.addExtra(timeComponent);
			rootComponent.addExtra(amountComponent);
			rootComponent.addExtra(infoComponent);

			String hoverText = ChatColor.GOLD + "Time: " + ChatColor.AQUA + time + " ago";
			hoverText += "\n" + ChatColor.GOLD + "Info: " + ChatColor.AQUA + log.getInfo();
			long quantity = log.getQuantity();
			if (quantity == 1L) {
				hoverText += "\n" + ChatColor.GOLD + "Amount: " + (total < 0 ? ChatColor.RED : ChatColor.GREEN) + totalString;
			} else {
				double single = log.getAmount();
				String singleString = SiggiEconomy.moneyToString(single);
				hoverText += "\n" + ChatColor.GOLD + "Amount: " + (total < 0 ? ChatColor.RED : ChatColor.GREEN) + singleString + ChatColor.AQUA + " x" + quantity;
				hoverText += "\n" + ChatColor.GOLD + "Total: " + (total < 0 ? ChatColor.RED : ChatColor.GREEN) + totalString;
			}
			hoverText += "\n" + ChatColor.GOLD + "New Balance: " + ChatColor.AQUA + SiggiEconomy.moneyToString(log.getNewBalance());
			hoverText += "\n" + ChatColor.GOLD + "Plugin: " + ChatColor.AQUA + log.getPlugin();

			rootComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverText)));

			sender.spigot().sendMessage(rootComponent);
		} else {
			String amountStr = SiggiEconomy.moneyToString(log.getTotalAmount());
			long quantity = log.getQuantity();
			if (quantity != 1) {
				amountStr += " (" + SiggiEconomy.moneyToString(log.getAmount()) + " x" + quantity + ")";
			}
			String time = timeToString(now - log.getTime());
			sender.sendMessage(ChatColor.GREEN + "" + time + ", " + amountStr + ", " + log.getInfo());
		}
	}

}
