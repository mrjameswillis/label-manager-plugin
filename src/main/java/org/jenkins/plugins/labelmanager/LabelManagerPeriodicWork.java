package org.jenkins.plugins.labelmanager;

import hudson.Extension;
import hudson.model.Node;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.jenkins.plugins.labelmanager.hudson.model.AsyncPeriodicWork;
import org.jenkins.plugins.labelmanager.model.LabelCron;
import org.jenkins.plugins.labelmanager.model.type.NodeType;
import org.jenkins.plugins.labelmanager.model.type.ResourceType;
import org.jenkins.plugins.labelmanager.model.type.TypeInterface;
import org.jenkins.plugins.labelmanager.utils.JenkinsUtils;
import org.jenkins.plugins.labelmanager.utils.Utils;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class LabelManagerPeriodicWork extends AsyncPeriodicWork {
    private static final Logger LOGGER = Logger.getLogger(LabelManagerPeriodicWork.class.getName());

    private final LabelManagerPluginImpl plugin = LabelManagerPluginImpl.getInstance();

    public LabelManagerPeriodicWork() {
        super("Label Manager Worker Thread");
    }

    @Override
    public long getRecurrencePeriod() {
        return MIN; // over a min diff we forget
    }

    @Override
    protected void execute(final TaskListener arg0) {
        final long currentTime = System.currentTimeMillis();
        //final LabelCron forceLabelCron = new LabelCron("Every 2 min", "H/2 * * * *", LabelAction.REPLACE, "test");
        for (LabelCron cron : plugin.getCrons()) {
            final LabelCron c = getNextScheduledCron(currentTime, cron);
            if (c != null) {
                runCronTask(c);
            }
        }
    }

    public void runCronTask(final LabelCron c) {
        final Jenkins instance = Jenkins.getActiveInstance();
        final boolean inQuietModeBeforeBackup = instance.isQuietingDown();

        try {
            // loop through all type and apply changes
            for (TypeInterface type : c.getTypes()) {
                if (type == null) {
                    LOGGER.log(Level.WARNING, "Object trying to set with cron (" + c.getName() + ") is null, skipping...");
                } else if (type instanceof NodeType) {
                    Node node = (Node) type.get();
                    if (node == null) {
                        LOGGER.log(Level.SEVERE, "Node does not exist, list available; " + JenkinsUtils.getAllNodeNames());
                        continue;
                    }
                    String newLabels = getResultingStringFromCron(c, node.getLabelString());
                    LOGGER.log(Level.FINE, "Setting node (" + node.getNodeName() + ") labels from [" + node.getLabelString() + "] to [" + newLabels + "]");
                    node.setLabelString(newLabels);
                    if (((NodeType) type).getNumberOfExecutors().isPresent()) {
                        LOGGER.log(Level.FINE, "Setting node (" + node.getNodeName() + ") executors from [" + node.getNumExecutors() + "] to [" + ((NodeType) type).getNumberOfExecutors().get() + "]");
                        JenkinsUtils.setNumExecutors(node, (int) ((NodeType) type).getNumberOfExecutors().get());
                    }
                } else if (type instanceof ResourceType) {
                    // LockableResource res = (LockableResource) type.get();
                    LOGGER.log(Level.SEVERE, "Setting LockableResource (" + type + ") not supported.");
                } else {
                    LOGGER.log(Level.SEVERE, "Object is not of supported type!");
                }
            }
        } catch (final Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to set labels with an exception!", ex);
        } finally {
            if (!inQuietModeBeforeBackup)
                instance.doCancelQuietDown();
            else
                LOGGER.log(Level.FINE, "Process finished, but was in quiet mode before running, remaining there...");
        }
    }

    public String getResultingStringFromCron(final LabelCron c, final String currentLabelString) {
        Set<String> hs = new HashSet<>(Arrays.asList(currentLabelString.split(" ")));
        switch (c.getLabelAction()) {
            case REPLACE:
                hs = new HashSet<>(c.getLabels());
                break;
            case ADD:
                hs.addAll(c.getLabels());
                break;
            case REMOVE:
                hs.removeAll(c.getLabels());
                break;
        }
        return String.join(" ", hs).trim();
    }

    public LabelCron getNextScheduledCron(final long currentTime, final LabelCron... cArray) {
        return getNextScheduledCron(currentTime, Arrays.asList(cArray));
    }

    public LabelCron getNextScheduledCron(final long currentTime, final List<LabelCron> cList) {
        // find all the bad crons and ignore them (shouldn't happen often)
        Map<LabelCron, Long> cMap = new LinkedHashMap<>();
        for (LabelCron c : cList) {
            long cDelay = Utils.calculateDelay(currentTime, c);
            if (cDelay != -1) {
                cMap.put(c, cDelay);
            }
        }
        if (cMap.isEmpty()) {
            LOGGER.log(Level.WARNING, "Map is empty, returning null...");
            return null;
        }

        // we have to sort this list based on the diffs
        Optional<Map.Entry<LabelCron, Long>> val = cMap.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getValue))
                .findFirst();

        LOGGER.log(Level.FINE, "Return cron (" + val.get().getKey() + ") diff: " + val.get().getValue() + "\nMap: " + cMap);
        return val.get().getValue() < MIN ? val.get().getKey() : null;
    }
}
