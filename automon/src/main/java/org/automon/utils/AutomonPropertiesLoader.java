package org.automon.utils;

import java.io.*;
import java.util.Properties;

/**
 * Load Automon properties.  The order of loading is to look in the file named automon.properties in the classpath.
 * Next look for system properties passed to the command line: (-DdistributedDataRefreshRateInMinutes=10).
 * These take precedence over the file.  If properties aren't in the file or passed in via the command line
 * then use defaults.
 */
public class AutomonPropertiesLoader {

    private static final String EMPTY_DEFAULT_OPEN_MON = "";
    private String[] fileNames;
    private Properties automonProps;

    // AspectJ property that specifies the xml config file used by AspectJ
    private static final String ASPECTJ_CONFIG_FILE = "org.aspectj.weaver.loadtime.configuration";
    private static final String DEFAULT_PROPS_CONFIG_FILE = "automon.properties";
    private static final String DEFAULT_XML_CONFIG_FILE1 = "ajc-aop.xml";
    private static final String DEFAULT_XML_CONFIG_FILE2 = "aop.xml";

    public static final String CONFIGURED_OPEN_MON = "org.automon";

    private boolean configFileFound = false;

    // Simply gets system properties but put in class so it can be mocked in a test.
    SysProperty sysProperty = new SysProperty();

    public AutomonPropertiesLoader() {
        this(System.getProperty(ASPECTJ_CONFIG_FILE, null), DEFAULT_PROPS_CONFIG_FILE, DEFAULT_XML_CONFIG_FILE1, DEFAULT_XML_CONFIG_FILE2);
    }

    /**
     * @param fileNames list of file names to look for config properties in.  They are checked in order.  If none are found defaults are used.
     */
    public AutomonPropertiesLoader(String... fileNames) {
        this.fileNames = fileNames;
    }

    AutomonPropertiesLoader(SysProperty sysProperty, String... fileNames) {
        this(fileNames);
        this.sysProperty = sysProperty;
    }


    /**
     * Using logic documented in the class comments load properties.  Note it can't fail as in the worst case
     * it loads defaults.
     *
     * @return default properties and/or any properties passed in by the user
     */
    public Properties getProperties() {
        if (automonProps == null) {
            initialize();
        }
        return automonProps;
    }


    void initialize() {
        // note precedence is -D properties, then from the file, then defaults.
        Properties defaults = getDefaults();
        Properties userProvided = propertyLoader(fileNames);
        replaceWithSystemProps(userProvided);
        automonProps = new Properties(defaults);
        automonProps.putAll(userProvided);
    }


    private Properties propertyLoader(String[] fileNames) {
        Properties properties = new Properties();
        for (String fileName : fileNames) {
            if (fileName != null) { // happens if ASPECTJ_CONFIG_FILE wasn't passed
                properties = propertyLoader(Utils.stripFileScheme(fileName)); // aspectJ command line props start with file:
                if (configFileFound) {
                    return properties;
                }
            }
        }

        configFileFound = false;
        return properties;
    }


    // Try to load the fileName to see if it is there and has properties.
    private Properties propertyLoader(String fileName) {
        Properties properties = new Properties();
        InputStream input = null;
        try {
            // command line aop.xml file
            input = getConfigFileInputStream(fileName);
            if (input != null) {
                properties.load(input);
                configFileFound = true;
            }
        } catch (Throwable t) {
            // want to ignore exception and proceed with loading with CLI props or defaults.
        } finally {
            close(input);
        }

        return properties;
    }

    private InputStream getConfigFileInputStream(String fileName) throws FileNotFoundException {
        InputStream input = null;
        if (new File(fileName).exists()) {
            input = new BufferedInputStream(new FileInputStream(fileName));
        } else {
            input = getClass().getClassLoader().getResourceAsStream(fileName);
        }
        return input;
    }

    void close(InputStream input) {
        try {
            if (input != null) {
                input.close();
            }
        } catch (Throwable t) {

        }
    }

    /**
     * Use any properties that were passed in at the command line or defined at the OS
     */
    private void replaceWithSystemProps(Properties properties) {
        properties.putAll(sysProperty.getProperties());
    }

    /**
     * Defaults used if no config file is found
     */
    Properties getDefaults() {
        Properties defaults = new Properties();
        defaults.put(CONFIGURED_OPEN_MON, EMPTY_DEFAULT_OPEN_MON);
        return defaults;
    }

    static class SysProperty {
        public String getProperty(String key) {
            return System.getProperty(key);
        }

        public Properties getProperties() {
            return System.getProperties();
        }
    }

}
