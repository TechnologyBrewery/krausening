package org.bitbucket.krausening;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.lang3.StringUtils;

public class KrauseningWarSpecificBootstrapContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext servletContext = event.getServletContext();
        String overrideExtensionLocation = servletContext.getInitParameter(Krausening.OVERRIDE_EXTENSIONS_LOCATION);

        if (StringUtils.isNotBlank(overrideExtensionLocation)) {
            Krausening krausening = Krausening.getInstance();
            krausening.setOverrideExtensionsLocation(overrideExtensionLocation);
            krausening.loadProperties();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // nothing to do when the context is destroyed

    }

}
