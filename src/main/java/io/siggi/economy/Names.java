package io.siggi.economy;

import com.google.common.base.Charsets;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static io.siggi.economy.util.IO.*;

public class Names {

	private final File file;
	private final File tmpFile;
	private final File importFile;

	private final Map<UUID, String> uuidToName = new HashMap<>();
	private final Map<String, UUID> nameToUuid = new HashMap<>();
	private FileOutputStream out;

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
	private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

	public Names(File file, File importFile) {
		this.file = file;
		this.tmpFile = new File(file.getParentFile(), file.getName() + ".sav");
		this.importFile = importFile;
		if (file.exists()) {
			load();
		}
		try {
			out = new FileOutputStream(file, true);
		} catch (IOException e) {
		}
		if (importFile.exists() && importUuids()) {
			importFile.delete();
		}
	}

	public static Names get() {
		return SiggiEconomy.getInstance().getNames();
	}

	public String getName(UUID uuid) {
		try {
			readLock.lock();
			return uuidToName.get(uuid);
		} finally {
			readLock.unlock();
		}
	}

	public UUID getUUID(String name) {
		try {
			readLock.lock();
			return nameToUuid.get(name.toLowerCase());
		} finally {
			readLock.unlock();
		}
	}

	void set(UUID uuid, String name) {
		try {
			writeLock.lock();
			String oldName = uuidToName.get(uuid);
			if (oldName != null) {
				if (oldName.equals(name)) return;
				nameToUuid.remove(oldName.toLowerCase(), uuid);
			}
			UUID oldUuid = nameToUuid.get(name.toLowerCase());
			if (oldUuid != null) {
				uuidToName.remove(oldUuid, name);
			}
			uuidToName.put(uuid, name);
			nameToUuid.put(name.toLowerCase(), uuid);
			if (out != null) {
				try {
					writeEntry(out, uuid, name);
				} catch (Exception e) {
				}
			}
		} finally {
			writeLock.unlock();
		}
	}

	public void autofill(String part, Collection<String> suggestions) {
		try {
			readLock.lock();
			String partLower = part.toLowerCase();
			for (Map.Entry<String, UUID> entries : nameToUuid.entrySet()) {
				if (suggestions.size() >= 100) {
					break;
				}
				if (entries.getKey().startsWith(partLower)) {
					suggestions.add(uuidToName.get(entries.getValue()));
				}
			}
		} finally {
			readLock.unlock();
		}
	}

	private void load() {
		try {
			try (FileInputStream in = new FileInputStream(file)) {
				while (readEntry(in)) ;
			}
			long maxFileSize = Math.max(16L * 16L * ((long) nameToUuid.size()) * 2L, (16L * 16L * ((long) nameToUuid.size())) + 16384L);
			if (file.length() > maxFileSize) {
				resave();
			}
		} catch (Exception e) {
		}
	}

	private boolean importUuids() {
		try {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(importFile), StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					int equalPos = line.indexOf("=");
					if (equalPos == -1) continue;
					String key = line.substring(0, equalPos);
					String val = line.substring(equalPos + 1);
					try {
						UUID uuid = UUID.fromString(key.replaceAll("-", "").replaceAll("([0-9A-Fa-f]{8})([0-9A-Fa-f]{4})([0-9A-Fa-f]{4})([0-9A-Fa-f]{4})([0-9A-Fa-f]{12})", "$1-$2-$3-$4-$5"));
						set(uuid, val);
					} catch (Exception e) {
					}
				}
			}
			return true;
		} catch (IOException e) {
		}
		return false;
	}

	private void resave() {
		try {
			try (FileOutputStream out = new FileOutputStream(tmpFile)) {
				for (Map.Entry<UUID, String> entry : uuidToName.entrySet()) {
					writeEntry(out, entry.getKey(), entry.getValue());
				}
			}
			tmpFile.renameTo(file);
		} catch (IOException e) {
		}
	}

	private boolean readEntry(InputStream in) throws IOException {
		if (in.available() == 0) {
			return false;
		}
		long most = readLong(in);
		long least = readLong(in);
		UUID uuid = new UUID(most, least);
		String name = readString(in);
		set(uuid, name);
		return true;
	}

	private final ByteArrayOutputStream outBaos = new ByteArrayOutputStream();
	private void writeEntry(OutputStream out, UUID uuid, String name) throws IOException {
		outBaos.reset();
		writeLong(outBaos, uuid.getMostSignificantBits());
		writeLong(outBaos, uuid.getLeastSignificantBits());
		writeString(outBaos, name);
		outBaos.writeTo(out);
	}

	void close() {
		try {
			writeLock.lock();
			out.close();
		} catch (Exception e) {
		} finally {
			writeLock.unlock();
			out = null;
		}
	}

	public static UUID offlineUuid(String name) {
		return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(Charsets.UTF_8));
	}
}
