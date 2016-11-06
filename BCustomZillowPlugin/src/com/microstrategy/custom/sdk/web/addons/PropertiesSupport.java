package com.microstrategy.custom.sdk.web.addons;

import java.io.IOException;
import java.util.Properties;

public class PropertiesSupport {
	private Properties properties;

	private static PropertiesSupport instance;

	/**
	 * Private class constructor.
	 */
	private PropertiesSupport() {

		properties = new Properties();
		try {
			properties
					.load(this
							.getClass()
							.getResourceAsStream(
									"/properties/configuration.properties"));
		} catch (IOException e) {

		}
	}

	/**
	 * Singleton - returns instance of PropertiesSupport
	 * 
	 * @return PropertiesSupport
	 */

	public static PropertiesSupport getInstance() {
		if (instance == null) {
			instance = new PropertiesSupport();
		}
		return instance;
	}

	/**
	 * Get property
	 * 
	 * @param property
	 *            - Property provided as an input.
	 * @return String - property from security.properties file
	 */
	public String getProperty(String property) {
		return (properties.getProperty(property));
	}
}
