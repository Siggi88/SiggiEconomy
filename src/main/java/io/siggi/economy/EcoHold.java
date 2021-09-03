package io.siggi.economy;

import static io.siggi.economy.util.Util.processInfo;
import java.lang.ref.WeakReference;

public final class EcoHold {

	private final EcoUser user;
	private final long amount;
	private final long quantity;
	private final String plugin;
	private String rawInfo;
	private String info;
	WeakReference<EcoHold> ref;

	EcoHold(EcoUser user, long amount, long quantity, String plugin, String info) {
		this.user = user;
		this.amount = amount;
		this.quantity = quantity;
		this.plugin = plugin;
		this.rawInfo = info;
		this.info = processInfo(info);
	}

	public EcoUser getUser() {
		return user;
	}

	public double getAmount() {
		return ((double) amount) / 100.0;
	}

	long getLAmount() {
		return amount;
	}

	public long getQuantity() {
		return quantity;
	}

	public String getPlugin() {
		return plugin;
	}
	
	public void setInfo(String info){
		if (info==null)throw new NullPointerException();
		this.rawInfo = info;
		this.info = processInfo(info);}

	public String getRawInfo() {
		return rawInfo;
	}

	public String getInfo() {
		return info;
	}

	/**
	 * Check if the hold is still active.
	 *
	 * @return
	 */
	public boolean isActive() {
		return user.isHoldActive(this);
	}

	/**
	 * Capture the hold, debiting the held funds from the player's account.
	 */
	public EcoTransactionResult capture() {
		return user.captureHold(this);
	}

	/**
	 * Capture the hold, debiting the held funds from the player's account.
	 */
	public EcoTransactionResult capture(String info) {
		return user.captureHold(this, info);
	}

	/**
	 * Release the hold, making the funds available again.
	 */
	public void release() {
		user.releaseHold(this);
	}
}
