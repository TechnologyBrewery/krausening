package org.aeonbits.owner;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;

import org.aeonbits.owner.KrauseningConfig.KrauseningMergePolicy;
import org.aeonbits.owner.KrauseningConfig.KrauseningMergePolicy.KrauseningMergePolicyType;
import org.aeonbits.owner.KrauseningConfig.KrauseningSources;

import com.ask.krausening.Krausening;

/**
 * {@link KrauseningAwarePropertiesManager} replaces the default URL-based property file specification strategy that is
 * implemented in {@link PropertiesManager} and delegates to {@link Krausening} for loading property files. All of the
 * features present in OWNER, such as property variable expansion, default property values, hot reloading property
 * files, etc. are still supported. In addition, developers may still use the {@link Sources} annotation in conjunction
 * with the {@link KrauseningSources} annotation on the same interface in order to load *.properties with Krausening and
 * *.xml properties using OWNER.
 */
class KrauseningAwarePropertiesManager extends PropertiesManager {

	private static final long serialVersionUID = 8372096321097307057L;

	private List<String> krauseningPropertyFileNames;
	private KrauseningMergePolicyType mergePolicyType;
	private boolean ownerPropertySourcesSpecified;
	private HotReloadLogic krauseningHotReloadLogic;

	KrauseningAwarePropertiesManager(Class<? extends Config> clazz, Properties properties,
			ScheduledExecutorService scheduler, VariablesExpander expander, LoadersManager loaders, Map<?, ?>[] imports) {
		super(clazz, properties, scheduler, expander, loaders, imports);

		KrauseningSources krauseningSources = clazz.getAnnotation(KrauseningSources.class);
		if (krauseningSources == null || krauseningSources.value().length == 0) {
			throw new IllegalArgumentException("No @KrauseningSources were defined on " + clazz.getCanonicalName());
		}

		this.krauseningPropertyFileNames = Arrays.asList(krauseningSources.value());

		KrauseningMergePolicy mergePolicy = clazz.getAnnotation(KrauseningMergePolicy.class);
		this.mergePolicyType = mergePolicy != null ? mergePolicy.value()
				: KrauseningMergePolicyType.FAIL_ON_DUPLICATE_PROPERTY_KEY;

		this.ownerPropertySourcesSpecified = clazz.getAnnotation(Sources.class) != null;

		HotReload krauseningHotReload = clazz.getAnnotation(HotReload.class);
		if (krauseningHotReload != null) {
			try {
				this.krauseningHotReloadLogic = new HotReloadLogic(krauseningHotReload,
						getKrauseningPropertyFileURLs(this.krauseningPropertyFileNames), this);
			} catch (MalformedURLException exception) {
				throw new RuntimeException("Could not configure hot reload support for Krausening files", exception);
			}

			if (this.krauseningHotReloadLogic.isAsync())
				scheduler.scheduleAtFixedRate(new Runnable() {
					public void run() {
						krauseningHotReloadLogic.checkAndReload();
					}
				}, krauseningHotReload.value(), krauseningHotReload.value(), krauseningHotReload.unit());
		}
	}

	/**
	 * First loads properties specified in {@link KrauseningSources} annotations using {@link Krausening} followed by
	 * any properties specified in {@link Sources} annotations using OWNER and then merges all loaded properties using
	 * the specified {@link KrauseningMergePolicy}.
	 * 
	 * @return merged set of {@link Properties} specified by the relevant {@link KrauseningSources} and {@link Sources}
	 *         annotations.
	 */
	@Override
	Properties doLoad() {
		List<Properties> propertiesToMerge = new ArrayList<Properties>(this.krauseningPropertyFileNames.size());
		for (String krauseningPropertyFileName : this.krauseningPropertyFileNames) {
			propertiesToMerge.add(Krausening.getInstance().getProperties(krauseningPropertyFileName));
		}
		if (this.ownerPropertySourcesSpecified) {
			propertiesToMerge.add(super.doLoad());
		}
		return this.mergePolicyType.mergeProperties(propertiesToMerge);
	}

	/**
	 * Checks to see if properties files loaded by {@link Krausening} and OWNER have been physically updated and if so,
	 * reloads them.
	 */
	@Override
	void syncReloadCheck() {
		if (this.krauseningHotReloadLogic != null && this.krauseningHotReloadLogic.isSync()) {
			this.krauseningHotReloadLogic.checkAndReload();
		}
		if (this.ownerPropertySourcesSpecified) {
			super.syncReloadCheck();
		}
	}

	/**
	 * Reloads all properties managed by {@link Krausening} and OWNER.
	 */
	@Delegate
	@Override
	public void reload() {
		Krausening.getInstance().loadProperties();
		super.reload();
	}

	/**
	 * Returns a list of URLs that map to the actual physical property files managed by {@link Krausening}.
	 * 
	 * @param krauseningPropertyFileNames
	 *            file names of the properties that are managed by {@link Krausening}.
	 * @return {@link List} of {@link URL}s that map to the physical property files managed by {@link Krausening}.
	 * @throws MalformedURLException
	 */
	private List<URL> getKrauseningPropertyFileURLs(List<String> krauseningPropertyFileNames)
			throws MalformedURLException {
		List<URL> krauseningPropertyFileURLs = new ArrayList<URL>();
		List<File> krauseningFolders = Arrays.asList(new File(System.getProperty(Krausening.BASE_LOCATION)), new File(
				System.getProperty(Krausening.EXTENSIONS_LOCATION)));

		for (String krauseningPropertyFileName : krauseningPropertyFileNames) {
			for (File krauseningFolder : krauseningFolders) {
				File krauseningPropertyFileObj = new File(krauseningFolder, krauseningPropertyFileName);
				if (krauseningPropertyFileObj.exists()) {
					krauseningPropertyFileURLs.add(krauseningPropertyFileObj.toURI().toURL());
				}
			}
		}
		return krauseningPropertyFileURLs;
	}
}
