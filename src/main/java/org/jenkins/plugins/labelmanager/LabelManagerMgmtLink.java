package org.jenkins.plugins.labelmanager;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.ManagementLink;
import java.io.IOException;
import java.io.Serializable;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;

import org.jenkins.plugins.labelmanager.model.LabelCron;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

@Extension
public class LabelManagerMgmtLink extends ManagementLink implements Describable<LabelManagerMgmtLink> {

    private final LabelManagerPluginImpl manager;

    public LabelManagerMgmtLink() {
        manager = LabelManagerPluginImpl.get();
    }

    @Override
    public String getDisplayName() {
        return "Node Label Manager";
    }

    @Override
    public String getDescription() {
        return "Set node and resource labels on a schedule.";
    }

    public LabelManagerPluginImpl getManager() {
        return manager;
    }

    @RequirePOST
    public void doConfigSubmit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        Jenkins.getActiveInstance().checkPermission(Jenkins.ADMINISTER);
        manager.configure(req, req.getSubmittedForm());
        //rsp.forwardToPreviousPage(req);
        rsp.sendRedirect("");
    }

    @Override
    public String getUrlName() {
        return "label-manager";
    }

    @Override
    public String getIconFileName() {
        return "/images/48x48/network.png";
    }

    @Override
    public Descriptor<LabelManagerMgmtLink> getDescriptor() {
        return Jenkins.getActiveInstance().getDescriptorOrDie(getClass());
    }

    public Descriptor<LabelCron> getLabelCronDescriptor() {
        return Jenkins.getActiveInstance().getDescriptorOrDie(LabelCron.class);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<LabelManagerMgmtLink> {

        @Override
        public String getDisplayName() {
            return null;
        }
    }
}
