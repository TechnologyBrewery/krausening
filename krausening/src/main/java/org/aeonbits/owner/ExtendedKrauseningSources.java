package org.aeonbits.owner;

import java.lang.annotation.Annotation;

import org.aeonbits.owner.KrauseningConfig.KrauseningSources;

/**
 * Provides a consistent mechanism to extend a KrauseningSources annotated config class by 
 * overwriting the properties file name to something new at runtime.  The primary use case
 * for this functionality is when you want to build some generic code and need to have multiple
 * different property sets for the same concept deployed at the same time.
 * 
 * For instance, imagine a generic library that uses JDBC and has multiple deployments in a
 * single server.  There is no way to to use KrauseningSources and have the different deployments
 * use different sets of properties that point to different databases.  Using this approach, you can
 * call alterPropertyAnnocationName before creating your KrauseningConfig instance and effectively 
 * have multiple instances of the same config class pointing to different versions of properties.
 * 
 * This can only be accomplished across classloaders.
 */
public class ExtendedKrauseningSources implements KrauseningSources {

    private String[] value;

    public ExtendedKrauseningSources(String value) {
        this.value = new String[] { value };
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return ExtendedKrauseningSources.class;
    }

    @Override
    public String[] value() {
        return this.value;
    }

}