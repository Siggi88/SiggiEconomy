package io.siggi.economy.baltop;

import java.util.UUID;

public final class TopUser {
	private final UUID user;
	private final double balance;
	public TopUser(UUID user, double balance) {
		this.user=user;
		this.balance=balance;
	}
	public UUID getUser(){return user;}
	public double getBalance(){return balance;}
}
