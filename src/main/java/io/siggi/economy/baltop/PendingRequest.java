package io.siggi.economy.baltop;

import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.command.CommandSender;

abstract class PendingRequest {

	private PendingRequest() {
	}

	static final class P extends PendingRequest {

		private final UUID user;
		private final int page;

		P(UUID user, int page) {
			this.user = user;
			this.page = page;
		}

		public UUID getUser() {
			return user;
		}

		public int getPage() {
			return page;
		}

		@Override
		public int hashCode() {
			return user.hashCode();
		}

		@Override
		public boolean equals(Object other) {
			if (other instanceof P) {
				return this.user.equals(((P) other).user);
			}
			return false;
		}
	}

	static final class C extends PendingRequest {

		private final CommandSender sender;
		private final int page;

		C(CommandSender sender, int page) {
			this.sender = sender;
			this.page = page;
		}

		public CommandSender getSender() {
			return sender;
		}

		public int getPage() {
			return page;
		}

		@Override
		public int hashCode() {
			return sender.hashCode();
		}

		@Override
		public boolean equals(Object other) {
			if (other instanceof C) {
				C c = (C) other;
				return sender.equals(c.sender);
			}
			return false;
		}
	}

	static final class S extends PendingRequest {

		private final Consumer<BalTopSnapshot> consumer;

		S(Consumer<BalTopSnapshot> consumer) {
			this.consumer = consumer;
		}

		public Consumer<BalTopSnapshot> getConsumer() {
			return consumer;
		}
	}
}
