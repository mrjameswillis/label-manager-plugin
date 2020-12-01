package org.jenkins.plugins.labelmanager.utils;

import hudson.model.*;
import jenkins.model.Jenkins;
import org.jenkins.plugins.labelmanager.model.type.NodeType;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class JenkinsUtils {

    private static final Logger LOGGER = Logger.getLogger(JenkinsUtils.class.getName());

    private static final int QUIETMODE_MONITORING_SLEEP = 500;

    /**
     * Get the Jenkins home (root) directory.
     *
     * @return
     *      Jenkins home directory as @File
     */
    public static File getJenkinsHome() {
        return Jenkins.getActiveInstance().getRootDir();
    }

    public static List<String> getAllNodeNamesForJelly() {
        List<String> nodes = new LinkedList<>();
        for (String name : JenkinsUtils.getAllNodeNames()) {
            if (name.contains(" ")) name = "\"" + name + "\"";
            nodes.add(name);
        }
        return nodes;
    }

    public static List<String> getAllNodeNames() {
        List<String> nodes = Jenkins.getActiveInstance().getNodes().stream().map(Node::getDisplayName).collect(Collectors.toCollection(LinkedList::new));
        nodes.add(NodeType.MASTER_NODE);
        return nodes;
    }

    /**
     * Waits until all Jenkins slaves are idle.
     */
    public static void waitUntilIdle() {
        Jenkins instance = Jenkins.getActiveInstance();
        final Computer computers[] = instance.getComputers();

        boolean running;
        do {
            running = false;
            for (final Computer computer : computers) {
                if (computer.countBusy() != 0) {
                    running = true;
                    break;
                }
            }

            try {
                Thread.sleep(QUIETMODE_MONITORING_SLEEP);
            } catch (final InterruptedException e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
            }
        } while (running);
    }

    /**
     * Waits until all executors are idle and switch Jenkins to quiet mode. If it takes to long that all executors are
     * idle because in the mean time other jobs are executed the timeout ensure that the quiet mode is forced.
     *
     * @param timeout
     *          specifies when a quiet mode is forced. 0 = no timeout.
     * @param unit
     *          specifies the time unit for the value of timeout.
     *
     * @throws IOException
     *          throws an io exception when the file is not found
     */
    public static void waitUntilIdleAndSwitchToQuietMode(int timeout, TimeUnit unit) throws IOException {
        Jenkins instance = Jenkins.getActiveInstance();
        final Computer computers[] = instance.getComputers();

        boolean running;
        long starttime = System.currentTimeMillis();
        do {
            running = false;
            for (final Computer computer : computers) {
                if (computer.countBusy() != 0) {
                    running = true;
                    break;
                }
            }

            try {
                TimeUnit.MILLISECONDS.sleep(QUIETMODE_MONITORING_SLEEP);
            } catch (final InterruptedException e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
            }

            if (!instance.isQuietingDown() && starttime + unit.toMillis(timeout) < System.currentTimeMillis()) {
                LOGGER.fine("Force quiet mode for jenkins now and wait until all executors are idle.");
                instance.doQuietDown();
            }
        } while (running);
    }

    public static boolean isNodeOnline(String nodeName) {
        if (NodeType.MASTER_NODE.equals(nodeName)) {
            return true;
        }

        final Computer c = Jenkins.getActiveInstance().getComputer(nodeName);
        if (c != null) {
            Node n = c.getNode();
            // really check if the node is available for execution
            return n != null && c.isOnline() && c.getNumExecutors() > 0;
        }
        return false;
    }

    public static synchronized void setNumExecutors(final Node node, int newNumOfExecutors) throws Exception {
        final Jenkins instance = Jenkins.getActiveInstance();
        final hudson.slaves.SlaveComputer c = (hudson.slaves.SlaveComputer) node.toComputer();
        if (c == null || c.getExecutors() == null) return;
        final int diff = c.getExecutors().size()-newNumOfExecutors;

        //Slave s = new Slave();
        //instance.addNode()

        if (diff > 0) {
            // we have too many executors
            // send signal to all idle executors to potentially kill them off
            // need the Queue maintenance lock held to prevent concurrent job assignment on the idle executors
            Queue.withLock(() -> {
                c.getExecutors().stream().filter(Executor::isIdle).forEach(Executor::interrupt);
            });
            //instance.setNumExecutors(instance.getNumExecutors() - diff);
        } else if (diff < 0) {
            // if the number is increased, add new ones
            Set<Integer> availableNumbers  = new HashSet<>();
            for (int i = 0; i < newNumOfExecutors; i++)
                availableNumbers.add(i);

            for (Executor executor : c.getExecutors())
                availableNumbers.remove(executor.getNumber());

            /* There may be busy executors with higher index, so only
               fill up until numExecutors is reached.
               Extra executors will call removeExecutor(...) and that
               will create any necessary executors from #0 again. */
            availableNumbers.stream().filter(number -> c.getExecutors().size() < newNumOfExecutors).forEach(number -> {
                Executor e = new Executor(c, number);
                c.getExecutors().add(e);
            });
            //instance.setNumExecutors(instance.getNumExecutors() + (-1 * diff));
        }
        //instance.removeNode(node);
        //instance.addNode(node);

        instance.setNodes(instance.getNodes());
        instance.save();
    }
}
