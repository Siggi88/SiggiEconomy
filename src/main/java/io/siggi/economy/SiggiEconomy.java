package io.siggi.economy;

import io.siggi.economy.baltop.EcoBalTop;
import io.siggi.economy.commands.CommandBal;
import io.siggi.economy.commands.CommandBalTop;
import io.siggi.economy.commands.CommandEco;
import io.siggi.economy.commands.CommandPay;
import io.siggi.economy.commands.CommandTransactions;
import io.siggi.economy.util.Util;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SiggiEconomy extends JavaPlugin {

	private static SiggiEconomy instance;

	public static SiggiEconomy getInstance() {
		return instance;
	}

	private EcoBalTop ecoBalTop;
	private Names names;
	private EventListener listener;

	@Override
	public void onLoad() {
		instance = this;
	}

	@Override
	public void onEnable() {
		Util.getCallingPlugin(); // first call resolves reflection

		ecoBalTop = new EcoBalTop();

		PluginCommand ecC = getCommand("eco");
		CommandEco ce = new CommandEco(this);
		ecC.setExecutor(ce);
		ecC.setTabCompleter(ce);

		PluginCommand bC = getCommand("bal");
		CommandBal cb = new CommandBal(this);
		bC.setExecutor(cb);
		bC.setTabCompleter(cb);

		PluginCommand btC = getCommand("baltop");
		CommandBalTop cbt = new CommandBalTop(this, ecoBalTop);
		btC.setExecutor(cbt);
		btC.setTabCompleter(cbt);

		PluginCommand pC = getCommand("pay");
		CommandPay cp = new CommandPay(this);
		pC.setExecutor(cp);
		pC.setTabCompleter(cp);

		PluginCommand tC = getCommand("transactions");
		CommandTransactions ct = new CommandTransactions(this);
		tC.setExecutor(ct);
		tC.setTabCompleter(ct);

		File df = getDataFolder();
		if (!df.exists()) df.mkdirs();
		names = new Names(new File(df, "names.dat"), new File(df, "import-uuids.txt"));
		listener = new EventListener(this);
		getServer().getPluginManager().registerEvents(listener, this);
	}

	@Override
	public void onDisable() {
		ecoBalTop.interruptThread();
		names.close();
	}

	private final Map<UUID, WeakReference<EcoUser>> users = new HashMap<>();
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock readLock = lock.readLock();
	private final Lock writeLock = lock.writeLock();

	/**
	 * Get an EcoUser.
	 *
	 * @param uuid their UUID
	 * @return the EcoUser
	 */
	public static EcoUser getUser(UUID uuid) {
		return instance.getUser0(uuid);
	}

	private EcoUser getUser0(UUID uuid) {
		if (uuid == null) {
			throw new NullPointerException("uuid cannot be null");
		}
		Lock l = readLock;
		readLock.lock();
		try {
			WeakReference<EcoUser> ref = users.get(uuid);
			EcoUser user;
			if (ref == null || (user = ref.get()) == null) {
				readLock.unlock();
				l = writeLock;
				writeLock.lock();
				ref = users.get(uuid);
				if (ref == null || (user = ref.get()) == null) {
					cleanInvalid();
					users.put(uuid, new WeakReference<>(user = new EcoUser(uuid)));
				}
			}
			return user;
		} finally {
			l.unlock();
		}
	}

	/**
	 * Get a Set listing all known users with at least one transaction.
	 *
	 * @return
	 */
	public static Set<UUID> getKnownUsers() {
		return instance.getKnownUsers0();
	}

	private Set<UUID> getKnownUsers0() {
		File userDir = new File(getDataFolder(), "users");
		File[] ff = userDir.listFiles();
		if (ff == null) {
			return Collections.EMPTY_SET;
		}
		Set<UUID> set = new HashSet<>();
		for (File f : ff) {
			try {
				String name = f.getName();
				if (name.endsWith(".txt")) {
					UUID uuid = Util.uuidFromString(name.substring(0, name.length() - 4));
					set.add(uuid);
				}
			} catch (Exception e) {
			}
		}
		return Collections.unmodifiableSet(set);
	}

	private void cleanInvalid() {
		for (Iterator<Map.Entry<UUID, WeakReference<EcoUser>>> it = users.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, WeakReference<EcoUser>> entry = it.next();
			if (entry.getValue().get() == null) {
				it.remove();
			}
		}
	}

	public Names getNames() {
		return names;
	}

	public boolean migrateTransactions(UUID oldUuid, UUID newUuid) {
		File df = getDataFolder();
		File dir = new File(df, "users");
		String oldUuidString = oldUuid.toString().toLowerCase().replace("-", "");
		String newUuidString = newUuid.toString().toLowerCase().replace("-", "");
		File oldTx = new File(dir, oldUuidString + ".txt");
		File oldIdx = new File(dir, oldUuidString + ".idx");
		File newTx = new File(dir, newUuidString + ".txt");
		File newIdx = new File(dir, newUuidString + ".idx");
		if (oldTx.exists() && !newTx.exists()
				&& oldIdx.exists() && !newIdx.exists()) {
			oldTx.renameTo(newTx);
			oldIdx.renameTo(newIdx);
			return true;
		}
		return false;
	}
}
