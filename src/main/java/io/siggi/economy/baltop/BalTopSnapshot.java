package io.siggi.economy.baltop;

import java.util.Collections;
import java.util.List;

public final class BalTopSnapshot {

	private final List<TopUser> topUsers;
	private final long time;
	private final double totalMoney;

	BalTopSnapshot(List<TopUser> topUsers, long time, double totalMoney) {
		this.topUsers = Collections.unmodifiableList(topUsers);
		this.time = time;
		this.totalMoney = totalMoney;
	}

	public List<TopUser> getTopUsers() {
		return topUsers;
	}

	public long getLastUpdate() {
		return time;
	}

	public double getTotalMoney() {
		return totalMoney;
	}
}
