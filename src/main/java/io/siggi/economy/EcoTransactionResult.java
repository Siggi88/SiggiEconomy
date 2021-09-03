package io.siggi.economy;

public final class EcoTransactionResult {

	private final EcoUser user;
	private final EcoTransactionLog log;
	private final Result result;

	EcoTransactionResult(EcoUser user, EcoTransactionLog log, Result result) {
		if (user == null || log == null || result == null) {
			throw new NullPointerException();
		}
		this.user = user;
		this.log = log;
		this.result = result;
	}

	public EcoUser getUser() {
		return user;
	}

	public EcoTransactionLog getLog() {
		return log;
	}

	public Result getResult() {
		return result;
	}

	public boolean isSuccessful() {
		return result == Result.OK || result == Result.NO_CHANGE;
	}

	public enum Result {

		OK("The transaction was successful."),
		NO_CHANGE("No change was made."),
		NOT_ENOUGH_FUNDS("Not enough funds are available to complete this transaction."),
		TOO_MUCH_MONEY("Too much funds, SiggiEconomy only supports up to 1 trillion."),
		DISK_ERROR("The transaction could not be written to disk."),
		CLOCK_ERROR("The clock appears to have rolled backwards, the timestamp of the most recent transaction is a later date."),
		HOLD_INVALID("The hold you tried to capture has already been captured or released."),
		NEGATIVE_AMOUNT("A negative amount was specified.");

		private final String description;

		private Result(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}
}
