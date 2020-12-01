package org.jenkins.plugins.labelmanager;

import hudson.Extension;
import hudson.Plugin;
import hudson.XmlFile;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkins.plugins.labelmanager.model.LabelCron;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class LabelManagerPluginImpl extends Plugin {

    private static final Logger LOGGER = Logger.getLogger(LabelManagerPluginImpl.class.getName());

    private LinkedHashSet<LabelCron> crons;
    private static LabelManagerPluginImpl instance = null;

    public LabelManagerPluginImpl() {
        crons = new LinkedHashSet<>();
        setInstance(this);
    }

    static void setInstance(LabelManagerPluginImpl instance) {
        LabelManagerPluginImpl.instance = instance;
    }

    @Override
    public void start() throws Exception {
        super.start();
        try {
            load();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Unable to load plugin configuration!");
            throw ex;
        }
    }

    @Override
    public synchronized void configure(StaplerRequest req, JSONObject json) {
        List<LabelCron> allCrons = new LinkedList<>();
        for (LabelCron c : req.bindJSONToList(LabelCron.class, json.get("cronsInList"))) {
            // lets validate the cron
            if (!LabelCron.isLabelCronValid(c)) continue;
            // now lets do any post config
            //c.getTypes().forEach(TypeInterface::configure);
            allCrons.add(c);
            Optional<LabelCron> old = crons.stream().filter(a -> a == c).findFirst();
            if (old.isPresent()) {
                LOGGER.finest("Found an existing cron: " + old.get().getName());
                // do we want to update any behind the scenes stuff?
            }
        }
        LOGGER.finest("All valid crons: " + allCrons);
        crons.clear();
        crons.addAll(allCrons);
        if (crons.isEmpty()) LOGGER.finest(json.toString());
        save();
    }

    @Override
    public synchronized void save() {
        try {
            super.save();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Unable to save plugin configuration!", ex);
        }
    }

    @Override
    @Nonnull
    protected XmlFile getConfigXml() {
        File f = new File(Jenkins.getActiveInstance().getRootDir(), this.getClass().getName() + ".xml");
        return new XmlFile(Jenkins.XSTREAM, f);
    }

    public Collection<LabelCron> getCrons() {
        return crons;
    }

    public static LabelManagerPluginImpl getInstance() {
        return instance;
    }

    public static LabelManagerPluginImpl get() {
        return Jenkins.getActiveInstance().getPlugin(LabelManagerPluginImpl.class);
    }
}
