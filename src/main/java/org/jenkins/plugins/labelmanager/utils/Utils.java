package org.jenkins.plugins.labelmanager.utils;

import antlr.ANTLRException;
import hudson.scheduler.CronTab;
import hudson.util.TimeUnit2;
import org.apache.commons.lang.StringUtils;
import org.jenkins.plugins.labelmanager.model.LabelCron;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;

public class Utils {

    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());

    //LOGGER.fine("Wait until executors are idle to perform backup.");
    //Utils.waitUntilIdleAndSwitchToQuietMode(plugin.getForceQuietModeTimeout(), TimeUnit.MINUTES);

    public static long calculateDelay(final long currentTime, final LabelCron c) {
        try {
            if (StringUtils.isEmpty(c.getCron())) {
                return -1;
            }

            LOGGER.fine(MessageFormat.format("Converting {0}: {1}", c.getName(), c.getCron()));

            CronTab cronTab = new CronTab(c.getCron());

            final Calendar nextExecution = cronTab.ceil(currentTime);
            final long delay = nextExecution.getTimeInMillis() - currentTime;

            LOGGER.fine(MessageFormat.format("Current time: {0,date,medium} {0,time,long}. Next execution ({3}) in {2} seconds which is {1,date,medium} {1,time,long}",
                    new Date(currentTime), nextExecution.getTime(), TimeUnit2.MILLISECONDS.toSeconds(delay), c.getName()));

            if (delay < 0) {
                final String msg = "Delay is a negative number, which means the next execution is in the past! This happens for Hudson/Jenkins installations with version 1.395 or below. Please upgrade to fix this.";
                LOGGER.severe(msg);
                throw new IllegalStateException(msg);
            }

            return delay;
        } catch (final ANTLRException e) {
            LOGGER.warning(MessageFormat.format(
                    "Cannot parse the specified ''Backup schedule for {0} backups''. Check cron notation.", c.getName()));
            return -1;
        }
    }
}
