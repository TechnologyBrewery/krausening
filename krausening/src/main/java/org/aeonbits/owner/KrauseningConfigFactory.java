package org.aeonbits.owner;

import org.aeonbits.owner.loaders.Loader;
import org.technologybrewery.krausening.Krausening;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

/**
 * {@link KrauseningConfigFactory} is largely modeled after {@link ConfigFactory} and provides a simple, straightforward
 * adapter for creating {@link KrauseningConfig} proxies and largely delegates to an underlying
 * {@link KrauseningFactory} implementation. This class is the intended and expected entry point for creating
 * {@link KrauseningConfig} proxies.
 */
public final class KrauseningConfigFactory {
	private static final Factory INSTANCE = newInstance();

	private KrauseningConfigFactory() {
	}

	/**
	 * Returns a new instance of a config Factory object.
	 * 
	 * @return a new instance of a config Factory object.
	 */
	private static Factory newInstance() {
		ScheduledExecutorService scheduler = newSingleThreadScheduledExecutor(new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread result = new Thread(r);
				result.setDaemon(true);
				return result;
			}
		});
		return new KrauseningFactory(scheduler, new Properties());
	}

	/**
	 * Creates a {@link KrauseningConfig} instance from the specified interface.
	 * 
	 * @param clazz
	 *            the interface extending from {@link KrauseningConfig} that you want to instantiate.
	 * @param imports
	 *            additional variables to be used to resolve the properties.
	 * @param <T>
	 *            type of the interface.
	 * @return an object implementing the given interface, which maps methods to property values.
	 */
	public static <T extends KrauseningConfig> T create(Class<? extends T> clazz, Map<?, ?>... imports) {
		return INSTANCE.create(clazz, imports);
	}

	/**
	 * Set a property in the {@link KrauseningConfigFactory}. Those properties will be used to expand variables
	 * specified in the `@Source` annotation, or by the {@link KrauseningConfigFactory} to configure its own behavior.
	 * 
	 * @param key
	 *            the key for the property.
	 * @param value
	 *            the value for the property.
	 * @return the old value.
	 */
	public static String setProperty(String key, String value) {
		return INSTANCE.setProperty(key, value);
	}

	/**
	 * Those properties will be used to expand variables specified in the `@Source` annotation, or by the
	 * {@link KrauseningConfigFactory} to configure its own behavior.
	 * 
	 * @return the properties in the {@link KrauseningConfigFactory}
	 */
	public static Properties getProperties() {
		return INSTANCE.getProperties();
	}

	/**
	 * Those properties will be used to expand variables specified in the `@Source` annotation, or by the
	 * {@link KrauseningConfigFactory} to configure its own behavior.
	 * 
	 * @param properties
	 *            the properties to set in the config Factory.
	 */
	public static void setProperties(Properties properties) {
		INSTANCE.setProperties(properties);
	}

	/**
	 * Returns the value for a given property.
	 * 
	 * @param key
	 *            the key for the property
	 * @return the value for the property, or null if the property is not set.
	 */
	public static String getProperty(String key) {
		return INSTANCE.getProperty(key);
	}

	/**
	 * Clears the value for the property having the given key. This means, that the given property is removed.
	 * 
	 * @param key
	 *            the key for the property to remove.
	 * @return the old value for the given key, or null if the property was not set.
	 */
	public static String clearProperty(String key) {
		return INSTANCE.clearProperty(key);
	}

	/**
	 * Registers a loader to enables additional file formats. Currently, *.properties files are handled by
	 * {@link Krausening}, while *.xml files are handled by one of OWNER's {@link org.aeonbits.owner.loaders.XMLLoader}.
	 * 
	 * @param loader
	 *            the loader to register.
	 * @throws NullPointerException
	 *             if specified loader is null.
	 */
	public static void registerLoader(Loader loader) {
		INSTANCE.registerLoader(loader);
	}

}
