package org.bitbucket.krausening;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RequiredOverride {
    // Environments that require overriding the property or file. If none, the override is required all the time.
    // Should match the name of the Krausening extension(s)
    String[] environments() default "";
}
