package io.siggi.economy.commands;

import io.siggi.economy.SiggiEconomy;
import io.siggi.economy.baltop.EcoBalTop;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

public class CommandBalTop implements CommandExecutor, TabExecutor {

	private final SiggiEconomy plugin;
	private final EcoBalTop ecoBalTop;

	public CommandBalTop(SiggiEconomy plugin, EcoBalTop ecoBalTop) {
		this.plugin = plugin;
		this.ecoBalTop = ecoBalTop;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		int page = 1;
		if (args.length >= 1) {
			try {
				page = Integer.parseInt(args[0]);
			} catch (Exception e) {
			}
		}
		ecoBalTop.showTo(sender, page);
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		return new ArrayList<>();
	}

}
