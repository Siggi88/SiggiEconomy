package io.siggi.economy;

import java.util.Objects;

import static io.siggi.economy.util.Util.processInfo;

public class EcoTransactionLog {

	private final EcoUser user;
	private final long time;
	private final double amount;
	private final long quantity;
	private final String plugin;
	private final String rawInfo;
	private final String info;
	private final double newBalance;

	EcoTransactionLog(EcoUser user, long time, double amount, long quantity, String plugin, String info, double newBalance) {
		this.user = user;
		this.time = time;
		this.amount = amount;
		this.quantity = quantity;
		this.plugin = plugin;
		this.rawInfo = info;
		this.info = processInfo(info);
		this.newBalance = newBalance;
	}

	public EcoUser getUser() {
		return user;
	}

	public long getTime() {
		return time;
	}

	public double getAmount() {
		return amount;
	}

	public double getTotalAmount() {
		return amount * ((double) quantity);
	}

	public long getQuantity() {
		return quantity;
	}

	public String getPlugin() {
		return plugin;
	}

	public String getRawInfo() {
		return rawInfo;
	}

	public String getInfo() {
		return info;
	}

	public double getOldBalance() {
		return newBalance - (amount * ((double) quantity));
	}

	public double getNewBalance() {
		return newBalance;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EcoTransactionLog that = (EcoTransactionLog) o;
		return time == that.time &&
				Double.compare(that.amount, amount) == 0 &&
				quantity == that.quantity &&
				Double.compare(that.newBalance, newBalance) == 0 &&
				user.getUUID().equals(that.user.getUUID()) &&
				plugin.equals(that.plugin) &&
				Objects.equals(rawInfo, that.rawInfo) &&
				Objects.equals(info, that.info);
	}

	@Override
	public int hashCode() {
		return Objects.hash(user.getUUID(), time, amount, quantity, plugin, rawInfo, info, newBalance);
	}
}
