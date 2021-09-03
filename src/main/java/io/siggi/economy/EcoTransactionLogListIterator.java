package io.siggi.economy;

import io.siggi.economy.util.RafInputStream;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

class EcoTransactionLogListIterator implements ListIterator<EcoTransactionLog> {
	private final EcoUser user;
	private long earlyFilePointer = 0L;
	private long lateFilePointer = 0L;
	private int currentIndex = 0;
	private boolean indexIsBetweenEntries = true;
	private final ArrayList<EcoTransactionLog> logs = new ArrayList<>();

	EcoTransactionLogListIterator(EcoUser user, long startTime) {
		this.user = user;
		if (!user.dataFile.exists() || !user.idxFile.exists()) {
			earlyFilePointer = lateFilePointer = EcoUser.USERDATA_PREFIX_LENGTH;
			return;
		}
		try {
			long startDay = startTime / 86400000L;
			long startPos = -1L;
			try (RandomAccessFile idxF = new RandomAccessFile(user.idxFile, "r")) {
				idxF.seek(0L);
				while (startPos == -1L) {
					if (idxF.length() - idxF.getFilePointer() < 16) {
						break;
					}
					long day = idxF.readLong();
					long pos = idxF.readLong();
					if (day >= startDay) {
						startPos = pos;
					}
				}
			}
			if (startPos == -1L) {
				earlyFilePointer = lateFilePointer = Math.max(EcoUser.USERDATA_PREFIX_LENGTH, user.dataFile.length());
				return;
			}
			boolean oneInFuture = false;
			try (RandomAccessFile dataF = new RandomAccessFile(user.dataFile, "r")) {
				dataF.seek(startPos);
				earlyFilePointer = startPos;
				RafInputStream in = new RafInputStream(dataF);
				String line;
				while ((line = in.readLine()) != null) {
					EcoTransactionLog log = user.parseLog(line);
					if (log != null) {
						long t = log.getTime();
						logs.add(log);
						if (t >= startTime) {
							oneInFuture = true;
							break;
						}
					}
				}
				lateFilePointer = dataF.getFilePointer();
			}
			if (oneInFuture) {
				currentIndex = logs.size() - 1;
			} else {
				currentIndex = logs.size();
			}
		} catch (IOException ioe) {
			earlyFilePointer = lateFilePointer = EcoUser.USERDATA_PREFIX_LENGTH;
			currentIndex = 0;
		}
	}

	private boolean loadMorePrevious() {
		try {
			user.readLock.lock();
			if (earlyFilePointer == EcoUser.USERDATA_PREFIX_LENGTH) {
				return false;
			}
			long startAt = earlyFilePointer - 8192L;
			boolean skipLine = true;
			if (startAt <= EcoUser.USERDATA_PREFIX_LENGTH) {
				skipLine = false;
				startAt = EcoUser.USERDATA_PREFIX_LENGTH;
			}
			try (RandomAccessFile raf = new RandomAccessFile(user.dataFile, "r")) {
				raf.seek(startAt);
				RafInputStream rafInputStream = new RafInputStream(raf);
				if (skipLine) {
					rafInputStream.skipLine();
					startAt = raf.getFilePointer();
				}
				rafInputStream.setLimit((int) (earlyFilePointer - startAt));
				List<EcoTransactionLog> ecoTransactionLogs = readLogs(rafInputStream, -1);
				if (ecoTransactionLogs.isEmpty()) return false;
				logs.addAll(0, ecoTransactionLogs);
				currentIndex += ecoTransactionLogs.size();
				earlyFilePointer = startAt;
				return true;
			} catch (IOException e) {
				return false;
			}
		} finally {
			user.readLock.unlock();
		}
	}

	private boolean loadMoreNext() {
		try {
			user.readLock.lock();
			try (RandomAccessFile raf = new RandomAccessFile(user.dataFile, "r")) {
				RafInputStream rafInputStream = new RafInputStream(raf);
				raf.seek(lateFilePointer);
				List<EcoTransactionLog> ecoTransactionLogs = readLogs(rafInputStream, 100);
				if (ecoTransactionLogs.isEmpty()) return false;
				logs.addAll(ecoTransactionLogs);
				lateFilePointer = raf.getFilePointer();
				return true;
			} catch (IOException e) {
				return false;
			}
		} finally {
			user.readLock.unlock();
		}
	}

	private List<EcoTransactionLog> readLogs(RafInputStream rafInputStream, int limit) throws IOException {
		List<EcoTransactionLog> list = new LinkedList<>();
		while (limit < 0 || list.size() < limit) {
			String line = rafInputStream.readLine();
			if (line == null) break;
			EcoTransactionLog ecoTransactionLog = user.parseLog(line);
			if (ecoTransactionLog != null) {
				list.add(ecoTransactionLog);
			}
		}
		return list;
	}

	@Override
	public boolean hasNext() {
		if (currentIndex < logs.size() - 1 || (indexIsBetweenEntries && currentIndex == logs.size() - 1)) {
			return true;
		}
		return loadMoreNext();
	}

	@Override
	public EcoTransactionLog next() {
		if (currentIndex == logs.size() - 1) {
			if (!loadMoreNext()) {
				throw new NoSuchElementException();
			}
		}
		if (indexIsBetweenEntries) {
			indexIsBetweenEntries = false;
		} else {
			currentIndex += 1;
		}
		return logs.get(currentIndex);
	}

	@Override
	public boolean hasPrevious() {
		if (currentIndex > 0) {
			return true;
		}
		return loadMorePrevious();
	}

	@Override
	public EcoTransactionLog previous() {
		if (currentIndex == 0) {
			if (!loadMorePrevious()) {
				throw new NoSuchElementException();
			}
		}
		indexIsBetweenEntries = false;
		currentIndex -= 1;
		return logs.get(currentIndex);
	}

	@Override
	public int nextIndex() {
		throw new UnsupportedOperationException("Cannot provide index of log entries.");
	}

	@Override
	public int previousIndex() {
		throw new UnsupportedOperationException("Cannot provide index of log entries.");
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Log entries cannot be modified.");
	}

	@Override
	public void set(EcoTransactionLog ecoTransactionLog) {
		throw new UnsupportedOperationException("Log entries cannot be modified.");
	}

	@Override
	public void add(EcoTransactionLog ecoTransactionLog) {
		throw new UnsupportedOperationException("Log entries cannot be modified.");
	}
}
