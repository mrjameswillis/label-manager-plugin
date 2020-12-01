package org.jenkins.plugins.labelmanager;

import hudson.model.Label;
import hudson.model.Node;
import hudson.slaves.DumbSlave;
import hudson.slaves.SlaveComputer;
import hudson.tasks.BatchFile;
import hudson.tasks.Builder;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test Base class for all tests
 */
public class LabelManagerTestBase {

    private static final Logger LOGGER = Logger.getLogger(LabelManagerTestBase.class.getName());

    @Rule
    public JenkinsRule j = null;

    public LabelManagerTestBase() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        //if (j != null)
         //   manager = j.jenkins.getPlugin(LockableResourcesManager.class);
    }

    @After
    public void tearDown() throws Exception {
    }

    public static String generateUniqueID() {
        return UUID.randomUUID().toString();
    }

    public Node createOnlineNode(String name) throws Exception {
        return createOnlineNode(name, "");
    }
    public Node createOnlineNode(String name, String labels) throws Exception {
        return createSlaveComputer(name, labels).getNode();
    }

    public SlaveComputer createSlaveComputer(String name) throws Exception {
        return createSlaveComputer(name, "");
    }
    public SlaveComputer createSlaveComputer(String name, String labels) throws Exception {
        DumbSlave s = j.createOnlineSlave();
        // setting name is not working right now
        //s.setNodeName(name);
        s.setLabelString(labels);
       return (SlaveComputer) s.createComputer();
    }
    public void assertLabelsMatch(String l1, String l2) {
        assertLabelsMatch(Arrays.asList(l1.split(" ")), Arrays.asList(l2.split(" ")));
    }
    public void assertLabelsMatch(List<String> l1, List<String> l2) {
        Assert.assertTrue("Labels from list 1 [" + l1 + "] do not match list 2 [" + l2 + "]", l1.containsAll(l2));
    }

    public void waitForNodeToComeOnline(Node node) {
        boolean ready = false;
        while (!ready) {
            try {
                Thread.sleep(2000);
                ready = node.toComputer().isOnline();
            } catch (Exception ex) {
                ready = false;
            }
        }
    }
}
