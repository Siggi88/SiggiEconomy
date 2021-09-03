package io.siggi.economy;

import io.siggi.economy.util.RafInputStream;
import io.siggi.economy.util.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class EcoUser {

	public static final long USERDATA_PREFIX_LENGTH = 84L;

	private long balance = 0L;
	private long totalCredits = 0L;
	private long totalDebits = 0L;
	private final UUID uuid;
	final File dataFile;
	final File idxFile;
	private final Set<WeakReference<EcoHold>> holds = new HashSet<>();
	final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	final Lock readLock = lock.readLock();
	final Lock writeLock = lock.writeLock();

	EcoUser(UUID uuid) {
		if (uuid == null) {
			throw new NullPointerException();
		}
		this.uuid = uuid;
		File dataDir = SiggiEconomy.getInstance().getDataFolder();
		File userDir = new File(dataDir, "users");
		if (!userDir.exists()) {
			userDir.mkdirs();
		}
		String uuidStr = uuid.toString().replace("-", "").toLowerCase();
		dataFile = new File(userDir, uuidStr + ".txt");
		idxFile = new File(userDir, uuidStr + ".idx");
		if (dataFile.exists()) {
			try (RandomAccessFile raf = new RandomAccessFile(dataFile, "r")) {
				byte[] balanceBytes = new byte[20];

				raf.seek(0L);
				raf.readFully(balanceBytes);
				String str = new String(balanceBytes);
				balance = Util.parsePaddedStringAsLong(str);

				raf.seek(21L);
				raf.readFully(balanceBytes);
				str = new String(balanceBytes);
				totalCredits = Util.parsePaddedStringAsLong(str);

				raf.seek(42L);
				raf.readFully(balanceBytes);
				str = new String(balanceBytes);
				totalDebits = Util.parsePaddedStringAsLong(str);
			} catch (IOException e) {
			}
		}
	}

	/**
	 * Get this user's UUID.
	 *
	 * @return this user's UUID
	 */
	public UUID getUUID() {
		return uuid;
	}

	/**
	 * Get this user's current balance.
	 *
	 * @return this user's current balance
	 */
	public double getBalance() {
		readLock.lock();
		try {
			return ((double) balance) / 100.0;
		} finally {
			readLock.unlock();
		}
	}

	public double getAvailableBalance() {
		readLock.lock();
		try {
			return ((double) (balance - getTotalHold0())) / 100.0;
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Get the total amount that has entered this user's account.
	 *
	 * @return the total amount that has entered this user's account
	 */
	public double getTotalCredits() {
		readLock.lock();
		try {
			return ((double) totalCredits) / 100.0;
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Get the total amount that has left this user's account.
	 *
	 * @return the total amount that has left this user's account
	 */
	public double getTotalDedits() {
		readLock.lock();
		try {
			return ((double) totalDebits) / 100.0;
		} finally {
			readLock.unlock();
		}
	}

	public EcoTransactionResult deposit(double amount, String info) {
		return deposit(amount, 1L, info);
	}

	public EcoTransactionResult deposit(double amount, long quantity, String info) {
		if (amount < 0.0) {
			String pluginName = Util.getCallingPluginName();
			if (pluginName == null) {
				pluginName = "Unknown";
			}
			EcoTransactionLog ecoTransactionLog = new EcoTransactionLog(this, System.currentTimeMillis(), 0.0, quantity, pluginName, info, getBalance());
			return new EcoTransactionResult(this, ecoTransactionLog, EcoTransactionResult.Result.NEGATIVE_AMOUNT);
		}
		return performTransaction(amount, quantity, info, false);
	}

	public EcoTransactionResult withdraw(double amount, String info) {
		return withdraw(amount, 1L, info);
	}

	public EcoTransactionResult withdraw(double amount, long quantity, String info) {
		return withdraw(amount, quantity, info, false);
	}

	public EcoTransactionResult withdraw(double amount, long quantity, String info, boolean allowNegativeBalance) {
		if (amount < 0.0) {
			String pluginName = Util.getCallingPluginName();
			if (pluginName == null) {
				pluginName = "Unknown";
			}
			EcoTransactionLog ecoTransactionLog = new EcoTransactionLog(this, System.currentTimeMillis(), 0.0, quantity, pluginName, info, getBalance());
			return new EcoTransactionResult(this, ecoTransactionLog, EcoTransactionResult.Result.NEGATIVE_AMOUNT);
		}
		return performTransaction(-amount, quantity, info, allowNegativeBalance);
	}

	/**
	 * Perform a transaction, if the amount is positive it will be a credit, if
	 * the amount is negative it will be a debit.
	 *
	 * @param amount the amount
	 * @param info a description of this transaction
	 * @return the result
	 */
	public EcoTransactionResult performTransaction(double amount, String info) {
		return performTransaction(amount, 1L, info);
	}

	/**
	 * Perform a transaction, if the amount is positive it will be a credit, if
	 * the amount is negative it will be a debit.
	 *
	 * @param amount the unit amount
	 * @param quantity the quantity
	 * @param info a description of this transaction
	 * @return the result
	 */
	public EcoTransactionResult performTransaction(double amount, long quantity, String info) {
		return performTransaction(amount, quantity, info, false);
	}

	/**
	 * Perform a transaction, if the amount is positive it will be a credit, if
	 * the amount is negative it will be a debit.
	 *
	 * @param amount the unit amount
	 * @param quantity the quantity
	 * @param info a description of this transaction
	 * @param allowNegativeBalance whether to allow the user balance to go
	 * negative or not
	 * @return the result
	 */
	public EcoTransactionResult performTransaction(double amount, long quantity, String info, boolean allowNegativeBalance) {
		String pluginName = Util.getCallingPluginName();
		if (pluginName == null) {
			pluginName = "Unknown";
		}
		if (info == null) {
			info = pluginName;
			if (info.equals("Essentials")) {
				allowNegativeBalance = true;
			}
		}
		String eInfo = info.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
		long now = System.currentTimeMillis();
		long day = now / 86400000L;
		boolean isCharge = amount < 0;
		long longAmount = ((long) (Math.abs(amount) * 100.0));
		writeLock.lock();
		try {
			long newBalance = isCharge ? (balance - (longAmount * quantity)) : (balance + (longAmount * quantity));
			long newCredits = isCharge ? totalCredits : (totalCredits + (longAmount * quantity));
			long newDebits = isCharge ? (totalDebits + (longAmount * quantity)) : totalDebits;
			EcoTransactionLog log = new EcoTransactionLog(this, now, amount, quantity, pluginName, info, ((double) newBalance) / 100.0);
			if (((long) (amount * 100.0)) == 0L || quantity == 0L) {
				return new EcoTransactionResult(this, log, EcoTransactionResult.Result.NO_CHANGE);
			}
			if (!allowNegativeBalance && isCharge && newBalance < getTotalHold0()) {
				return new EcoTransactionResult(this, log, EcoTransactionResult.Result.NOT_ENOUGH_FUNDS);
			} else if (!isCharge && newBalance > 100000000000000L) {
				return new EcoTransactionResult(this, log, EcoTransactionResult.Result.TOO_MUCH_MONEY);
			}
			try (RandomAccessFile dataF = new RandomAccessFile(dataFile, "rw")) {
				boolean isNewFile = dataF.length() < USERDATA_PREFIX_LENGTH;
				boolean essentialsSetMoneyWorkaround = false;
				EcoTransactionLog updatingTransaction = null;
				try (RandomAccessFile idxF = new RandomAccessFile(idxFile, "rw")) {
					long recentDay;
					long idxFLen = idxF.length();
					idxFLen -= idxFLen % 16;
					if (idxFLen == 0L) {
						recentDay = 0L;
					} else {
						idxF.seek(idxFLen - 16);
						recentDay = idxF.readLong();
					}
					if (recentDay > day) {
						return new EcoTransactionResult(this, log, EcoTransactionResult.Result.CLOCK_ERROR);
					}
					dataF.seek(0L);

					dataF.write(Util.longToPaddedString(newBalance).getBytes());
					dataF.write(0x0A);

					dataF.write(Util.longToPaddedString(newCredits).getBytes());
					dataF.write(0x0A);

					dataF.write(Util.longToPaddedString(newDebits).getBytes());
					dataF.write(0x0A);

					long latestTransaction = 0L;
					if (isNewFile) {
						dataF.write(Util.longToPaddedString(0L).getBytes());
						dataF.write(0x0A);
						latestTransaction = dataF.getFilePointer();
					} else {
						dataF.seek(63L);
						byte[] bbb = new byte[20];
						dataF.readFully(bbb);
						String str = new String(bbb);
						latestTransaction = Util.parsePaddedStringAsLong(str);
						dataF.seek(latestTransaction);
						try (BufferedReader reader = new BufferedReader(new InputStreamReader(new RafInputStream(dataF)))) {
							String l = reader.readLine();
							EcoTransactionLog ll = parseLog(l);
							if (ll != null) {
								if (ll.getTime() > now) {
									return new EcoTransactionResult(this, log, EcoTransactionResult.Result.CLOCK_ERROR);
								} else if (ll.getRawInfo().equals(info) && ((long) (ll.getAmount() * 100.0)) == ((long) (amount * 100.0)) && now - ll.getTime() < 300000L) {
									updatingTransaction = ll;
								} else if (ll.getRawInfo().equals(info) && quantity == 1L && ll.getQuantity() == 1L && ll.getNewBalance() == 0.0 && now - ll.getTime() < 1000L) {
									updatingTransaction = ll;
									essentialsSetMoneyWorkaround = true;
								}
							}
						}
						if (updatingTransaction != null) {
							dataF.seek(latestTransaction);
							dataF.setLength(latestTransaction);
						} else {
							dataF.seek(dataF.length());
						}
					}

					if (recentDay < day) {
						idxF.seek(idxFLen);
						idxF.writeLong(day);
						idxF.writeLong(dataF.length());
					}
				}
				String logLine;
				if (essentialsSetMoneyWorkaround) {
					long calculatedDiff = newBalance - ((long) (updatingTransaction.getOldBalance() * 100.0));
					//logLine = now + "," + calculatedDiff + "," + quantity + "," + newBalance + "," + eInfo;
					logLine = createLog(now, calculatedDiff, quantity, newBalance, pluginName, info);
				} else {
					//logLine = now + "," + (isCharge ? "-" : "") + longAmount + "," + (updatingTransaction == null ? quantity : (quantity + updatingTransaction.getQuantity())) + "," + newBalance + "," + eInfo;
					logLine = createLog(now, longAmount * (isCharge ? -1 : 1), (updatingTransaction == null ? quantity : (quantity + updatingTransaction.getQuantity())), newBalance, pluginName, info);
				}
				byte[] bytes = logLine.getBytes();

				long latestTransaction = dataF.getFilePointer();

				dataF.write(bytes);
				dataF.write(0x0A);

				dataF.seek(63L);
				dataF.write(Util.longToPaddedString(latestTransaction).getBytes());
				dataF.write(0x0A);
			} catch (IOException e) {
				return new EcoTransactionResult(this, log, EcoTransactionResult.Result.DISK_ERROR);
			}
			balance = newBalance;
			totalCredits = newCredits;
			totalDebits = newDebits;
			return new EcoTransactionResult(this, log, EcoTransactionResult.Result.OK);
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Get a transaction list iterator starting at a certain time.
	 *
	 * @param startTime The time to place the pointer
	 * @return list iterator of transactions
	 */
	public ListIterator<EcoTransactionLog> getTransactionLogs(long startTime) {
		return new EcoTransactionLogListIterator(this, startTime);
	}

	private String createLog(long time, long amount, long quantity, long newBalance, String plugin, String info) {
		return "V1," + time + "," + amount + "," + quantity + "," + newBalance + "," + plugin + "," + info;
	}

	EcoTransactionLog parseLog(String line) {
		String version = line.substring(0, line.indexOf(","));
		try {
			switch (version) {
				case "V1":
					String[] parts = line.split(",", 7);
					long time = Long.parseLong(parts[1]);
					long amountL = Long.parseLong(parts[2]);
					long quantity = Long.parseLong(parts[3]);
					long newBalL = Long.parseLong(parts[4]);
					String plugin = parts[5];
					String eInfo = parts[6];

					double amount = ((double) amountL) / 100.0;
					double newBal = ((double) newBalL) / 100.0;
					String info = eInfo.replace("\\r", "\r").replace("\\n", "\n").replace("\\\\", "\\");

					return new EcoTransactionLog(this, time, amount, quantity, plugin, info, newBal);
				default:
					break;
			}
		} catch (Exception e) {
		}
		return null;
	}

	/**
	 * Get the total amount of funds temporarily made unavailable in holds.
	 *
	 * @return amount that is unavailable
	 */
	public double getTotalHold() {
		readLock.lock();
		try {
			return ((double) getTotalHold0()) / 100.0;
		} finally {
			readLock.unlock();
		}
	}

	private long getTotalHold0() {
		long total = 0L;
		for (Iterator<WeakReference<EcoHold>> it = holds.iterator(); it.hasNext();) {
			WeakReference<EcoHold> ref = it.next();
			if (ref == null) {
				it.remove();
				continue;
			}
			EcoHold hold = ref.get();
			if (hold == null) {
				it.remove();
				continue;
			}
			total += hold.getLAmount() * hold.getQuantity();
		}
		return total;
	}

	/**
	 * Place a hold on funds if you don't know whether you'll need it or not,
	 * but need to ensure it is available for when it is needed.
	 *
	 * @param amount The unit amount to hold
	 * @param info The description for the transaction
	 * @return a hold object, or null if a hold could not be placed.
	 */
	public EcoHold placeHold(double amount, String info) {
		return placeHold(amount, 1L, info);
	}

	/**
	 * Place a hold on funds if you don't know whether you'll need it or not,
	 * but need to ensure it is available for when it is needed.
	 *
	 * @param amount The unit amount to hold
	 * @param quantity The quantity to hold
	 * @param info The description for the transaction
	 * @return a hold object, or null if a hold could not be placed.
	 */
	public EcoHold placeHold(double amount, long quantity, String info) {
		return placeHold(amount, quantity, info, false);
	}

	/**
	 * Place a hold on funds if you don't know whether you'll need it or not,
	 * but need to ensure it is available for when it is needed.
	 *
	 * @param amount The unit amount to hold
	 * @param quantity The quantity to hold
	 * @param info The description for the transaction
	 * @param allowNegativeBalance Allow the balance to go negative
	 * @return a hold object, or null if a hold could not be placed.
	 */
	public EcoHold placeHold(double amount, long quantity, String info, boolean allowNegativeBalance) {
		String pluginName = Util.getCallingPluginName();
		if (pluginName == null) {
			pluginName = "Unknown";
		}
		long lAmount = ((long) (amount * 100.0));
		if (lAmount <= 0 || quantity < 1) {
			return null;
		}
		writeLock.lock();
		try {
			if (!allowNegativeBalance) {
				long availableBalance = balance - getTotalHold0();
				if (availableBalance < lAmount) {
					return null;
				}
			}
			EcoHold hold = new EcoHold(this, lAmount, quantity, pluginName, info);
			WeakReference<EcoHold> ref = new WeakReference<>(hold);
			hold.ref = ref;
			holds.add(ref);
			return hold;
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Get all currently active holds.
	 *
	 * @return
	 */
	public List<EcoHold> getHolds() {
		readLock.lock();
		try {
			List<EcoHold> h = new ArrayList<>(holds.size());
			for (Iterator<WeakReference<EcoHold>> it = holds.iterator(); it.hasNext(); ) {
				WeakReference<EcoHold> ref = it.next();
				if (ref == null) {
					it.remove();
					continue;
				}
				EcoHold hold = ref.get();
				if (hold == null) {
					it.remove();
					continue;
				}
				h.add(hold);
			}
			return h;
		} finally {
			readLock.unlock();
		}
	}

	boolean isHoldActive(EcoHold hold) {
		readLock.lock();
		try {
			return holds.contains(hold.ref);
		} finally {
			readLock.unlock();
		}
	}

	EcoTransactionResult captureHold(EcoHold hold) {
		return captureHold(hold, hold.getRawInfo());
	}

	EcoTransactionResult captureHold(EcoHold hold, String info) {
		writeLock.lock();
		try {
			if (holds.contains(hold.ref)) {
				holds.remove(hold.ref);
				return performTransaction(-hold.getAmount(), hold.getQuantity(), info, true);
			} else {
				long now = System.currentTimeMillis();
				long newBalance = balance - (hold.getLAmount() * hold.getQuantity());
				EcoTransactionLog log = new EcoTransactionLog(this, now, -hold.getAmount(), hold.getQuantity(), hold.getPlugin(), info, ((double) newBalance) / 100.0);
				return new EcoTransactionResult(this, log, EcoTransactionResult.Result.HOLD_INVALID);
			}
		} finally {
			writeLock.unlock();
		}
	}

	void releaseHold(EcoHold hold) {
		writeLock.lock();
		try {
			holds.remove(hold.ref);
		} finally {
			writeLock.unlock();
		}
	}
}
