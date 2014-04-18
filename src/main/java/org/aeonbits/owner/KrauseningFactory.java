package org.aeonbits.owner;

import static java.lang.reflect.Proxy.newProxyInstance;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;

/**
 * {@link KrauseningFactory} extends {@link DefaultFactory} in order to delegate to
 * {@link KrauseningAwarePropertiesManager} for property mapper proxy generation.
 */
class KrauseningFactory extends DefaultFactory {

	private final ScheduledExecutorService scheduler;

	KrauseningFactory(ScheduledExecutorService scheduler, Properties props) {
		super(scheduler, props);
		this.scheduler = scheduler;
	}

	@Override
	public <T extends Config> T create(Class<? extends T> clazz, Map<?, ?>... imports) {
		Class<?>[] interfaces = new Class<?>[] { clazz };
		VariablesExpander expander = new VariablesExpander(getProperties());
		PropertiesManager manager = new KrauseningAwarePropertiesManager(clazz, new Properties(), scheduler, expander,
				loadersManager, imports);
		PropertiesInvocationHandler handler = new PropertiesInvocationHandler(manager);
		T proxy = (T) newProxyInstance(clazz.getClassLoader(), interfaces, handler);
		handler.setProxy(proxy);
		return proxy;
	}
}
