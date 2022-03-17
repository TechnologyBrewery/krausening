package org.aeonbits.owner;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * {@link KrauseningConfig} serves as the interface to extend in order to define
 * a property mapping interface. Annotations that are used to specify the
 * desired Krausening property files to load and merge policy are also
 * encapsulated within this interface.
 */
public interface KrauseningConfig extends Config, Accessible {

    /**
     * Specifies the Krausening property files that are desired for use within
     * the target property mapping interface
     * (i.e. @KrauseningSources("config.properties"))
     */
    @Retention(RUNTIME)
    @Target(TYPE)
    @Documented
    @Inherited
    public @interface KrauseningSources {
        String[] value();
    }

    /**
     * Specifies the merge policy to use when loading multiple property files
     * from Krausening.
     */
    @Retention(RUNTIME)
    @Target(TYPE)
    @Documented
    @Inherited
    public @interface KrauseningMergePolicy {
        KrauseningMergePolicyType value() default KrauseningMergePolicyType.FAIL_ON_DUPLICATE_PROPERTY_KEY;

        enum KrauseningMergePolicyType {
            FAIL_ON_DUPLICATE_PROPERTY_KEY {
                @Override
                Properties mergeProperties(List<Properties> propertiesList) {
                    Set<String> duplicatePropertyKeys = new HashSet<String>();
                    Properties mergedProperties = new Properties();
                    for (Properties properties : propertiesList) {
                        for (Object propertyKey : properties.keySet()) {
                            if (mergedProperties.containsKey(propertyKey)) {
                                duplicatePropertyKeys.add((String) propertyKey);
                            } else {
                                mergedProperties.put(propertyKey, properties.getProperty((String) propertyKey));
                            }
                        }
                    }
                    if (!duplicatePropertyKeys.isEmpty()) {
                        throw new RuntimeException("The following duplicate property key(s) were found: "
                                + StringUtils.join(duplicatePropertyKeys, ", "));
                    }
                    return mergedProperties;
                }
            },
            LAST_TAKES_PRECEDENCE {
                @Override
                Properties mergeProperties(List<Properties> propertiesList) {
                    Properties mergedProperties = new Properties();
                    for (Properties properties : propertiesList) {
                        mergeEncryptableProperties(properties, mergedProperties);
                    }
                    return mergedProperties;
                }
            },
            FIRST_TAKES_PRECEDENCE {
                @Override
                Properties mergeProperties(List<Properties> propertiesList) {
                    Properties mergedProperties = new Properties();
                    for (int reverseIterator = propertiesList.size() - 1; reverseIterator >= 0; reverseIterator--) {
                        mergeEncryptableProperties(propertiesList.get(reverseIterator), mergedProperties);
                    }
                    return mergedProperties;
                }
            };
            abstract Properties mergeProperties(List<Properties> propertiesList);

            /**
             * Puts all of the properties from the given merge source into the
             * target, taking into account that {@link Properties} from the
             * source may be encrypted.
             * 
             * @param mergeSource
             *            properties to merge into the target
             * @param mergeTarget
             *            target properties into which the source will be
             *            merged.
             */
            private static void mergeEncryptableProperties(Properties mergeSource, Properties mergeTarget) {
                for (Object keyObj : mergeSource.keySet()) {
                    String key = (String) keyObj;
                    mergeTarget.put(key, mergeSource.getProperty(key));
                }
            }
        }
    }
}
