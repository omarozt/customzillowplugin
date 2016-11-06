package com.microstrategy.custom.sdk.web.log;

import com.microstrategy.utils.log.Logger;
import com.microstrategy.utils.log.LoggerConfigurator;

/**
 * Logger class for plug-in.
 */
public class Log extends LoggerConfigurator {

    /**
     * logger instance.
     */
    public static final Logger logger = new Log().createLogger();
}
