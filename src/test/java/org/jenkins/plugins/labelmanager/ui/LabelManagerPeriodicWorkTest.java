package org.jenkins.plugins.labelmanager.ui;

import hudson.model.Node;
import hudson.slaves.DumbSlave;
import hudson.slaves.SlaveComputer;
import jenkins.model.Jenkins;
import org.jenkins.plugins.labelmanager.model.LabelAction;
import org.jenkins.plugins.labelmanager.model.LabelCron;
import org.jenkins.plugins.labelmanager.LabelManagerPeriodicWork;
import org.jenkins.plugins.labelmanager.LabelManagerTestBase;
import org.jenkins.plugins.labelmanager.model.type.NodeType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Arrays;
import java.util.logging.Logger;


public class LabelManagerPeriodicWorkTest extends LabelManagerTestBase {

    private LabelManagerPeriodicWork periodicWork;
    private long staticTimeMillis = 692890282000L;
    private static final Logger LOGGER = Logger.getLogger(LabelManagerPeriodicWorkTest.class.getName());

    public LabelManagerPeriodicWorkTest() {
        super();
        j = new JenkinsRule();
    }

    @Before
    public void setup() {
        periodicWork = new LabelManagerPeriodicWork();
    }

    @Test
    public void testRunCronTaskSingle() throws Exception {
        //SlaveComputer c = createSlaveComputer("test");
        DumbSlave s = j.createOnlineSlave();
        //s.setNodeName("test");
        SlaveComputer c = s.getComputer();
        Node before = c.getNode();
        LOGGER.info("using node name: " + before.getNodeName());
        LabelCron labelCron1 = new LabelCron("labelCron1", "* * * * *", LabelAction.REPLACE, "test test2 test3", new NodeType(before));
        //periodicWork.getNextScheduledCron(staticTimeMillis, labelCron1);
        periodicWork.runCronTask(labelCron1);
        Node after = c.getNode();
        //Assert.assertNotEquals(before, after);
        assertLabelsMatch("test test2 test3", after.getLabelString());
    }

    @Test
    public void testRunCronTaskMultiReplace() throws Exception {
        SlaveComputer c1 = createSlaveComputer("test1");
        Node n1 = c1.getNode();
        SlaveComputer c2 = createSlaveComputer("test2", "old");
        Node n2 = c2.getNode();
        LabelCron labelCron1 = new LabelCron("labelCron1", "* * * * *", LabelAction.REPLACE, "test new", new NodeType(n1), new NodeType(n2));
        //periodicWork.getNextScheduledCron(staticTimeMillis, labelCron1);
        periodicWork.runCronTask(labelCron1);
        assertLabelsMatch("test new", c1.getNode().getLabelString());
        assertLabelsMatch("test new", c2.getNode().getLabelString());
    }

    @Test
    public void testRunCronTaskMultiAdd() throws Exception {
        SlaveComputer c1 = createSlaveComputer("test1");
        Node n1 = c1.getNode();
        SlaveComputer c2 = createSlaveComputer("test2", "first");
        Node n2 = c2.getNode();
        LabelCron labelCron1 = new LabelCron("labelCron1", "* * * * *", LabelAction.ADD, "second", new NodeType(n1), new NodeType(n2));
        //periodicWork.getNextScheduledCron(staticTimeMillis, labelCron1);
        periodicWork.runCronTask(labelCron1);
        assertLabelsMatch("second", c1.getNode().getLabelString());
        assertLabelsMatch("first second", c2.getNode().getLabelString());
    }

    @Test
    public void testRunCronTaskMultiRemove() throws Exception {
        SlaveComputer c1 = createSlaveComputer("test1");
        Node n1 = c1.getNode();
        SlaveComputer c2 = createSlaveComputer("test2", "first second");
        Node n2 = c2.getNode();
        LabelCron labelCron1 = new LabelCron("labelCron1", "* * * * *", LabelAction.REMOVE, "second", new NodeType(n1), new NodeType(n2));
        //periodicWork.getNextScheduledCron(staticTimeMillis, labelCron1);
        periodicWork.runCronTask(labelCron1);
        assertLabelsMatch("", c1.getNode().getLabelString());
        assertLabelsMatch("first", c2.getNode().getLabelString());
    }

    @Test
    @Ignore
    public void testRunCronTaskMultiExecutors() throws Exception {
        SlaveComputer c1 = createSlaveComputer("test1");
        Node n1 = c1.getNode();
        SlaveComputer c2 = createSlaveComputer("test2", "first");
        Node n2 = c2.getNode();
        LabelCron labelCron1 = new LabelCron("labelCron1", "* * * * *", LabelAction.ADD, "second", new NodeType(n1, 4), new NodeType(n2, 2));
        //periodicWork.getNextScheduledCron(staticTimeMillis, labelCron1);
        periodicWork.runCronTask(labelCron1);
        Thread.sleep(10000);
        LOGGER.info("slept for 10 seconds... " + Jenkins.getActiveInstance().getNumExecutors());
        waitForNodeToComeOnline(n1);
        c1 = (SlaveComputer) Jenkins.getActiveInstance().getNode(n1.getNodeName()).toComputer();
        c2 = (SlaveComputer) Jenkins.getActiveInstance().getNode(n2.getNodeName()).toComputer();

        assertLabelsMatch("second", c1.getNode().getLabelString());
        assertLabelsMatch("first second", c2.getNode().getLabelString());
        Assert.assertEquals(4, c1.getNumExecutors());
        Assert.assertEquals(2, c2.getNumExecutors());
    }
}
