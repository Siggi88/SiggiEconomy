package io.siggi.economy.util;

import io.siggi.economy.Names;
import io.siggi.economy.SiggiEconomy;
import java.lang.reflect.Field;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.plugin.java.JavaPlugin;

public final class Util {

	private Util() {
	}

	private static Class<?> pluginClassLoader;
	private static Field pluginField;

	public static JavaPlugin getCallingPlugin() {
		if (pluginField == null) {
			try {
				pluginClassLoader = Class.forName("org.bukkit.plugin.java.PluginClassLoader");
				pluginField = pluginClassLoader.getDeclaredField("plugin");
				pluginField.setAccessible(true);
			} catch (Exception e) {
				return null;
			}
		}
		JavaPlugin result = null;
		StackTraceElement[] trace = new Throwable().getStackTrace();
		for (StackTraceElement element : trace) {
			String className = element.getClassName();
			if (className.startsWith("io.siggi.economy.commands.")){
				return SiggiEconomy.getInstance();
			}
			try {
				Class<?> cls = Class.forName(className);
				ClassLoader classLoader = cls.getClassLoader();
				if (pluginClassLoader.isAssignableFrom(classLoader.getClass())) {
					JavaPlugin plugin = (JavaPlugin) pluginField.get(classLoader);
					String name = plugin.getName();
					if (name.equals("SiggiEconomy") || name.equals("Vault")) {
						result = null;
					} else if (result == null) {
						result = plugin;
					}
				}
			} catch (Exception e) {
			}
		}
		return result;
	}

	public static String getCallingPluginName() {
		JavaPlugin callingPlugin = getCallingPlugin();
		return callingPlugin == null ? null : callingPlugin.getName();
	}

	public static long parsePaddedStringAsLong(String str) {
		str = str.replace("#", "");
		return Long.parseLong(str);
	}

	public static String longToPaddedString(long balance) {
		String str = Long.toString(balance);
		StringBuilder sb = new StringBuilder();
		for (int i = str.length(); i < 20; i++) {
			sb.append("#");
		}
		sb.append(str);
		return sb.toString();
	}

	public static UUID uuidFromString(String uuid) {
		return UUID.fromString(uuid.replace("-", "").replaceAll(
				"([0-9A-Fa-f]{8})([0-9A-Fa-f]{4})([0-9A-Fa-f]{4})([0-9A-Fa-f]{4})([0-9A-Fa-f]{12})",
				"$1-$2-$3-$4-$5"
		));
	}

	private static final Pattern userReplacement = Pattern.compile("\\[user:([0-9A-Fa-f\\-]*)\\]");

	public static String processInfo(String info) {
		try {
			info = replace(info, userReplacement, (m) -> Names.get().getName(Util.uuidFromString(m.group(1))));
		} catch (Exception e) {
		}
		return info;
	}

	private static String replace(String info, Pattern pattern, Function<Matcher, String> replacer) {
		Matcher matcher = pattern.matcher(info);
		while (matcher.find()) {
			String before = info.substring(0, matcher.start());
			String after = info.substring(matcher.end());
			String replacedWith = replacer.apply(matcher);
			info = before + replacedWith + after;
			matcher = pattern.matcher(info);
		}
		return info;
	}
}
