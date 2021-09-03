package io.siggi.economy.baltop;

import io.siggi.economy.EcoUser;
import io.siggi.economy.Names;
import io.siggi.economy.SiggiEconomy;
import io.siggi.economy.util.Util;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class EcoBalTop {
	
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock readLock = lock.readLock();
	private final Lock writeLock = lock.writeLock();
	private final List<TopUser> topUsers = new ArrayList<>();
	private final Set<PendingRequest> pendingRequests = new HashSet<>();
	private double totalMoney = 0.0;
	private long lastUpdate = 0L;
	private boolean currentlyUpdating = false;
	private Thread updaterThread = null;
	
	public EcoBalTop() {
	}

	/**
	 * Get the top users into a callback, the callback may be called on the
	 * current thread before this method exits, or from the main thread later
	 * on.
	 *
	 * @param consumer the callback
	 */
	public void getBalTop(Consumer<BalTopSnapshot> consumer) {
		boolean retryInWriteLock = false;
		BalTopSnapshot result = null;
		readLock.lock();
		try {
			if (currentlyUpdating || isStale()) {
				retryInWriteLock = true;
			} else {
				result = createSnapshot();
			}
		} finally {
			readLock.unlock();
		}
		if (retryInWriteLock) {
			writeLock.lock();
			try {
				if (currentlyUpdating) {
					PendingRequest.S req = new PendingRequest.S(consumer);
					pendingRequests.add(req);
				} else if (isStale()) {
					PendingRequest.S req = new PendingRequest.S(consumer);
					pendingRequests.add(req);
				} else {
					result = createSnapshot();
				}
			} finally {
				writeLock.unlock();
			}
		}
		if (result != null) {
			consumer.accept(result);
		}
	}
	
	public void showTo(CommandSender p, int page) {
		boolean retryInWriteLock = false;
		readLock.lock();
		try {
			if (currentlyUpdating || isStale()) {
				retryInWriteLock = true;
			} else {
				doShowTo(p, page);
			}
		} finally {
			readLock.unlock();
		}
		if (retryInWriteLock) {
			writeLock.lock();
			try {
				if (currentlyUpdating) {
					if (p instanceof Player) {
						PendingRequest.P req = new PendingRequest.P(((Player) p).getUniqueId(), page);
						pendingRequests.remove(req);
						pendingRequests.add(req);
					} else {
						PendingRequest.C req = new PendingRequest.C(p, page);
						pendingRequests.remove(req);
						pendingRequests.add(req);
					}
				} else if (isStale()) {
					if (p instanceof Player) {
						PendingRequest.P req = new PendingRequest.P(((Player) p).getUniqueId(), page);
						pendingRequests.remove(req);
						pendingRequests.add(req);
					} else {
						PendingRequest.C req = new PendingRequest.C(p, page);
						pendingRequests.remove(req);
						pendingRequests.add(req);
					}
					currentlyUpdating = true;
					startUpdaterThread();
				} else {
					doShowTo(p, page);
				}
			} finally {
				writeLock.unlock();
			}
		}
	}
	
	private void doShowTo(CommandSender p, int page) {
		int perPage = 8;
		int maxPage = (topUsers.size() + (perPage - 1)) / perPage;
		if (maxPage == 0) {
			p.sendMessage(ChatColor.GOLD + "Top Users (no results)");
		}
		if (page < 1 || page > maxPage) {
			p.sendMessage(ChatColor.GOLD + "Enter a page number between " + ChatColor.YELLOW + "1" + ChatColor.GOLD + " and " + ChatColor.YELLOW + maxPage + ChatColor.GOLD + ".");
			return;
		}
		p.sendMessage(ChatColor.GOLD + "Top Users (Server Economy: " + ChatColor.YELLOW + "$" + Util.moneyToString(totalMoney) + ChatColor.GOLD + ") (page " + ChatColor.YELLOW + page + ChatColor.GOLD + "/" + ChatColor.YELLOW + maxPage + ChatColor.GOLD + ")");
		int startAt = (page - 1) * perPage;
		int endAt = Math.min(startAt + perPage, topUsers.size());
		for (int i = startAt; i < endAt; i++) {
			int pos = (i + 1);
			TopUser u = topUsers.get(i);
			String name = Names.get().getName(u.getUser());
			if (name == null) {
				name = ChatColor.RED + "(" + u.getUser().toString().toLowerCase() + ")";
			}
			p.sendMessage(ChatColor.GOLD + Integer.toString(pos) + ". " + ChatColor.YELLOW + "$" + Util.moneyToString(u.getBalance()) + ChatColor.GOLD + " - " + name);
		}
		if (page < maxPage) {
			p.sendMessage(ChatColor.GOLD + "For next page, type " + ChatColor.YELLOW + "/baltop " + (page + 1));
		}
	}
	
	private BalTopSnapshot createSnapshot() {
		List<TopUser> l = new ArrayList<>();
		l.addAll(topUsers);
		return new BalTopSnapshot(l, lastUpdate, totalMoney);
	}
	
	private boolean isStale() {
		long now = System.currentTimeMillis();
		return now - lastUpdate > 600000L;
	}
	
	private void startUpdaterThread() {
		(updaterThread = new Thread(this::updaterThread, "EcoBalTopThread")).start();
	}
	
	private void updaterThread() {
		readLock.lock();
		try {
			if (!currentlyUpdating) {
				return;
			}
		} finally {
			readLock.unlock();
		}
		Set<UUID> knownUsers = SiggiEconomy.getKnownUsers();
		List<TopUser> list = new ArrayList<>(knownUsers.size());
		double calcTotal = 0.0;
		for (UUID uuid : knownUsers) {
			EcoUser user = SiggiEconomy.getUser(uuid);
			list.add(new TopUser(uuid, user.getBalance()));
			calcTotal += user.getBalance();
			if (Thread.interrupted()) {
				return;
			}
		}
		list.sort(orderUsers);
		writeLock.lock();
		try {
			if (Thread.interrupted() || !currentlyUpdating) {
				return;
			}
			topUsers.clear();
			topUsers.addAll(list);
			totalMoney = calcTotal;
			updaterThread = null;
			currentlyUpdating = false;
			new BukkitRunnable() {
				@Override
				public void run() {
					sendQueued();
				}
			}.runTask(SiggiEconomy.getInstance());
		} finally {
			writeLock.unlock();
		}
	}
	
	private void sendQueued() {
		writeLock.lock();
		try {
			if (currentlyUpdating) {
				return;
			}
			for (Iterator<PendingRequest> it = pendingRequests.iterator(); it.hasNext();) {
				PendingRequest value = it.next();
				it.remove();
				if (value instanceof PendingRequest.P) {
					PendingRequest.P pp = (PendingRequest.P) value;
					UUID user = pp.getUser();
					Player p = Bukkit.getPlayer(user);
					if (p != null) {
						doShowTo(p, pp.getPage());
					}
				} else if (value instanceof PendingRequest.C) {
					PendingRequest.C c = (PendingRequest.C) value;
					CommandSender p = c.getSender();
					if (p != null) {
						doShowTo(p, c.getPage());
					}
				} else if (value instanceof PendingRequest.S) {
					PendingRequest.S s = (PendingRequest.S) value;
					s.getConsumer().accept(createSnapshot());
				}
			}
		} finally {
			writeLock.unlock();
		}
	}
	
	public void interruptThread() {
		writeLock.lock();
		try {
			if (updaterThread != null) {
				updaterThread.interrupt();
				updaterThread = null;
			}
			if (currentlyUpdating) {
				currentlyUpdating = false;
			}
			pendingRequests.clear();
		} finally {
			writeLock.unlock();
		}
	}
	
	private final Comparator<TopUser> orderUsers = (u1, u2) -> {
		double b1 = u1.getBalance();
		double b2 = u2.getBalance();
		if (b1 > b2) {
			return -1;
		} else if (b2 > b1) {
			return 1;
		} else {
			return 0;
		}
	};
}
