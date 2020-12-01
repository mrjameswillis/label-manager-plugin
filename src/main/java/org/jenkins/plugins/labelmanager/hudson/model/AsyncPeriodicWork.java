package org.jenkins.plugins.labelmanager.hudson.model;

import hudson.model.PeriodicWork;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.util.StreamTaskListener;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;

import jenkins.model.Jenkins;

/**
 * Duplicated code from <i>hudson.model.AsyncPeriodicWork</i> to reduce the log levels in {@link #doRun()} from INFO to
 * FINEST so the logs are not spammed.
 *
 * All other functionality is exactly the same as in the original class.
 */
public abstract class AsyncPeriodicWork extends PeriodicWork {

    /**
     * Human readable name of the work.
     */
    public final String name;

    private Thread thread;

    protected AsyncPeriodicWork(final String name) {
        this.name = name;
    }

    /**
     * Schedules this periodic work now in a new thread, if one isn't already running.
     */
    @Override
    @SuppressWarnings("deprecation") // in this case we really want to use PeriodicWork.logger since it reports the impl class
    public final void doRun() {
        try {
            if ((thread != null) && thread.isAlive()) {
                logger.log(Level.WARNING, "{0} thread is still running. Execution aborted.", name);
                return;
            }
            thread = new Thread(() -> {
                logger.log(Level.FINEST, "Started {0}", name);
                final long startTime = System.currentTimeMillis();
                long stopTime;

                final StreamTaskListener l = createListener();
                try {
                    l.getLogger().printf("Started at %tc%n", new Date(startTime));
                    ACL.impersonate(ACL.SYSTEM);

                    execute(l);
                } catch (IOException e) {
                    e.printStackTrace(l.fatalError(e.getMessage()));
                } catch (InterruptedException e) {
                    e.printStackTrace(l.fatalError("aborted"));
                } finally {
                    stopTime = System.currentTimeMillis();
                    try {
                        l.getLogger().printf("Finished at %tc. %dms%n", new Date(stopTime), stopTime - startTime);
                    } finally {
                        l.closeQuietly();
                    }
                }
                logger.log(Level.FINEST, "Finished {0}. {1,number} ms", new Object[]{name, stopTime - startTime});
            }, name + " thread");
            thread.start();
        } catch (final Exception e) {
            logger.log(Level.SEVERE, name + " thread failed with error", e);
        }
    }

    @SuppressWarnings("deprecation") // in this case we really want to use PeriodicWork.logger since it reports the impl class
    protected StreamTaskListener createListener() {
        File f = getLogFile();
        if (!f.getParentFile().isDirectory()) {
            if (!f.getParentFile().mkdirs()) {
                logger.log(Level.WARNING, "Could not create directory {0}", f.getParentFile());
            }
        }
        try {
            return new StreamTaskListener(f);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    /**
     * Determines the log file that records the result of this task.
     *
     * @return
     *      The log file as @File.
     */
    protected File getLogFile() {
        return new File(Jenkins.getActiveInstance().getRootDir(),"logs/tasks/"+name+".log");
    }

    /**
     * Executes the task.
     *
     * @param listener
     *      Output sent will be reported to the users. (this work is TBD.)
     * @throws InterruptedException
     *      The caller will record the exception and moves on.
     * @throws IOException
     *      The caller will record the exception and moves on.
     */
    protected abstract void execute(TaskListener listener) throws IOException, InterruptedException;

}