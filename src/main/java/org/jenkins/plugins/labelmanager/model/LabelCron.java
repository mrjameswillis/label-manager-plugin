package org.jenkins.plugins.labelmanager.model;

import antlr.ANTLRException;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.mapper.Mapper;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.scheduler.CronTabList;
import hudson.scheduler.Hash;
import hudson.triggers.Messages;
import hudson.util.FormValidation;
import org.jenkins.plugins.labelmanager.model.type.NodeType;
import org.jenkins.plugins.labelmanager.model.type.TypeInterface;
import org.jenkins.plugins.labelmanager.utils.JenkinsUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import org.kohsuke.accmod.restrictions.suppressions.SuppressRestrictedWarnings;

import java.text.DateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * implements Serializable
 */
@ExportedBean(defaultVisibility = 999)
public class LabelCron extends AbstractDescribableImpl<LabelCron> {

    private static final Logger LOGGER = Logger.getLogger(LabelCron.class.getName());
    public  static final String LABEL_STRING_SPLIT = " ";

    private String name;
    private String cron;
    private LabelAction labelAction = LabelAction.REPLACE;
    @XStreamConverter(value=LabelConverter.class)
    private LinkedHashSet<String> labels;
    @XStreamConverter(value=TypeConverter.class)
    private LinkedHashSet<String> typeNames;
    transient private LinkedHashSet<TypeInterface> types;

    public LabelCron(String name, String cron, LabelAction labelAction, String labels) {
        this(name, cron, labelAction, labels, new LinkedHashSet<>());
    }

    @DataBoundConstructor
    public LabelCron(String name, String cron, LabelAction labelAction, String labelString, String typeString) {
        this.name = Util.fixEmptyAndTrim(name);
        this.cron = cron;
        this.labelAction = labelAction == null ? LabelAction.REPLACE : labelAction;
        this.labels = new LinkedHashSet<>(labelsFromString(Util.fixNull(labelString).trim()));
        this.typeNames = new LinkedHashSet<>(cleanTypeString(typeString));
        this.types = convertStringsToNodeTypes(typeNames);
    }

    public LabelCron(String name, String cron, LabelAction labelAction, String labelString, TypeInterface ... types) {
        this(name, cron, labelAction, labelString, new LinkedHashSet<>(Arrays.asList(types)));
    }

    //@DataBoundConstructor
    public LabelCron(String name, String cron, LabelAction labelAction, String labelString, LinkedHashSet<TypeInterface> types) {
        this.name = Util.fixEmptyAndTrim(name);
        this.cron = cron;
        this.labelAction = labelAction == null ? LabelAction.REPLACE : labelAction;
        this.labels = new LinkedHashSet<>(labelsFromString(Util.fixNull(labelString).trim()));
        this.typeNames = types == null ? new LinkedHashSet<>() : types.stream().map(TypeInterface::getName).collect(Collectors.toCollection(LinkedHashSet::new));
        this.types = types;
    }

    @Exported
    public String getName() {
        return name;
    }

    @Exported
    public String getCron() {
        return cron;
    }

    @Exported
    public LabelAction getLabelAction() {
        return labelAction;
    }

    @Exported
    public String getLabelString() {
        return String.join(" ", labels).trim();
    }

    @Exported
    public LinkedHashSet<String> getLabels() {
        return labels;
    }

    @Exported
    public String getTypeString() {
        return String.join(" ", typeNames).trim();
    }

    @Exported
    public LinkedHashSet<TypeInterface> getTypes() {
        return convertStringsToNodeTypes(typeNames);
    }

    public boolean addType(TypeInterface type) {
        return types.add(type);
    }

    public boolean addAllTypes(List<TypeInterface> types) {
        return this.types.addAll(types);
    }

    public boolean removeType(TypeInterface type) {
        return types.remove(type);
    }

    private LinkedHashSet<TypeInterface> convertStringsToNodeTypes(LinkedHashSet<String> typeNames) {
        return typeNames.stream().map(NodeType::new).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public String toString() {
        return name + typeNames + "->" + labelAction + labels;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LabelCron other = (LabelCron) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    public static boolean isLabelCronValid(LabelCron c) {
        FormValidation formValidation = LabelCron.getLabelCronFormValidation(c);
        LOGGER.log(Level.FINEST, "Cron " + c.getName() + " message[" + formValidation.kind + "]: " + formValidation.toString());
        return (formValidation.kind == FormValidation.Kind.OK);
    }

    public static FormValidation getLabelCronFormValidation(LabelCron c) {
        DescriptorImpl descriptor = (DescriptorImpl) c.getDescriptor();
        Collection<FormValidation> validations = new ArrayList<>();
        validations.add(descriptor.doCheckName(c.getName()));
        validations.add(descriptor.doCheckCron(c.getCron(), null));
        validations.add(descriptor.doCheckLabelString(c.getLabelString()));
        validations.add(descriptor.doCheckTypeString(c.getTypeString()));
        return FormValidation.aggregate(validations);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<LabelCron> {

        @Override
        public String getDisplayName() {
            return "Cron";
        }

        public FormValidation doCheckName(@QueryParameter String value) {
            value = Util.fixEmptyAndTrim(value);
            if (value == null) {
                return FormValidation.error("Name cannot be empty!");
            } else if (value.contains(" ")) {
                return FormValidation.error("Names cannot contain spaces!");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckCron(@QueryParameter String value, @AncestorInPath Item item) {
            try {
                CronTabList ctl = CronTabList.create(Util.fixNull(value), item != null ? Hash.from(item.getFullName()) : null);
                Collection<FormValidation> validations = new ArrayList<>();
                updateCronValidationsForSanity(validations, ctl);
                updateCronValidationsForNextRun(validations, ctl);
                return FormValidation.aggregate(validations);
            } catch (ANTLRException e) {
                if (value.trim().indexOf('\n')==-1 && value.contains("**"))
                    return FormValidation.error(Messages.TimerTrigger_MissingWhitespace());
                return FormValidation.error(e.getMessage());
            }
        }

        private void updateCronValidationsForSanity(Collection<FormValidation> validations, CronTabList ctl) {
            String msg = ctl.checkSanity();
            if(msg!=null)  validations.add(FormValidation.warning(msg));
        }

        @SuppressRestrictedWarnings(CronTabList.class)
        private void updateCronValidationsForNextRun(Collection<FormValidation> validations, CronTabList ctl) {
            Calendar prev = ctl.previous();
            Calendar next = ctl.next();
            if (prev != null && next != null) {
                DateFormat fmt = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL);
                validations.add(FormValidation.ok(Messages.TimerTrigger_would_last_have_run_at_would_next_run_at(fmt.format(prev.getTime()), fmt.format(next.getTime()))));
            } else {
                validations.add(FormValidation.warning(Messages.TimerTrigger_no_schedules_so_will_never_run()));
            }
        }

        public FormValidation doCheckLabelString(@QueryParameter String value) {
            value = Util.fixEmptyAndTrim(value);
            if (value == null) {
                return FormValidation.error("Label cannot be empty!");
            }
            return FormValidation.ok();
        }

        // Temp while using string
        public AutoCompletionCandidates doAutoCompleteTypeString(@QueryParameter String value) {
            AutoCompletionCandidates c = new AutoCompletionCandidates();
            value = Util.fixEmptyAndTrim(value);
            if (value != null) {
                for (String l : JenkinsUtils.getAllNodeNames()) {
                    if (l.startsWith(value)) c.add(l);
                }
            }
            return c;
        }

        // Temp while using string
        public FormValidation doCheckTypeString(@QueryParameter String value) {
            value = Util.fixEmptyAndTrim(value);
            if (value == null) return FormValidation.error("Node names cannot be empty!");
            List<String> nodeNameList = JenkinsUtils.getAllNodeNamesForJelly();
            Collection<FormValidation> validations = new ArrayList<>();
            List<TypeInterface> list = typesFromString(value);
            validations.addAll(list.stream().filter(type -> !JenkinsUtils.getAllNodeNames().contains(type.getName())).map(type -> FormValidation.error("Invalid node (" + type.getName() + "), choose from: " + nodeNameList)).collect(Collectors.toList()));
            return FormValidation.aggregate(validations);
        }
    }//end class

    public static class LabelConverter extends CollectionConverter {
        public LabelConverter(Mapper mapper) {
            super(mapper);
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            LinkedHashSet<String> labels;
            String labelString = reader.getValue();
            if (labelString != null && !reader.hasMoreChildren()) {
                labels = new LinkedHashSet<>();
                labels.addAll(labelsFromString(labelString.trim()));
            } else {
                try {
                    labels = (LinkedHashSet<String>)super.unmarshal(reader, context);
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Failed to load old labels, resetting to continue load...");
                    labels = new LinkedHashSet<>();
                }
            }
            return labels;
        }
    }

    private static List<String> labelsFromString(String labelString) {
        if (labelString == null || labelString.length() <= 0) return Collections.emptyList();
        return Arrays.asList(labelString.split(LABEL_STRING_SPLIT));
    }

    /**
     * This converter is temporary since we only have nodes with names
     */
    public static class TypeConverter extends CollectionConverter {
        public TypeConverter(Mapper mapper) {
            super(mapper);
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            LinkedHashSet<String> types;
            String labelString = reader.getValue();
            if (labelString != null && !reader.hasMoreChildren()) {
                types = new LinkedHashSet<>();
                types.addAll(typeNamesFromString(labelString.trim()));
            } else {
                try {
                    types = (LinkedHashSet<String>)super.unmarshal(reader, context);
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Failed to load old types, resetting to continue load...");
                    types = new LinkedHashSet<>();
                }
            }
            return types;
        }
    }

    private static List<TypeInterface> typesFromString(String typeString) {
        if (typeString == null || typeString.length() <= 0) return Collections.emptyList();
        return cleanTypeString(typeString).stream().map(NodeType::new).collect(Collectors.toCollection(LinkedList::new));
    }

    private static List<String> typeNamesFromString(String typeString) {
        if (typeString == null || typeString.length() <= 0) return Collections.emptyList();
        return cleanTypeString(typeString).stream().map(String::new).collect(Collectors.toCollection(LinkedList::new));
    }

    private static List<String> cleanTypeString(String typeString) {
        List<String> list = new LinkedList<>();
        Matcher m = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'").matcher(typeString);
        while (m.find())
            list.add(m.group()); //.replaceAll("^\"|\"$", "")
        return list;
    }

    private static final long serialVersionUID = 1L;
}
