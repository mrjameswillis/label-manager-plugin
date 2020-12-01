package org.jenkins.plugins.labelmanager.api;

import antlr.ANTLRException;
import hudson.scheduler.CronTab;
import org.jenkins.plugins.labelmanager.model.LabelAction;
import org.jenkins.plugins.labelmanager.model.LabelCron;
import org.jenkins.plugins.labelmanager.LabelManagerPeriodicWork;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Calendar;


public class LabelManagerPeriodicWorkTest {

    private LabelManagerPeriodicWork periodicWork;
    private long staticTimeMillis = 692890282000L;

    @Before
    public void setup() {
        periodicWork = new LabelManagerPeriodicWork();
    }

    @Test
    public void testGetNextScheduledCronEmpty() {
        LabelCron res = periodicWork.getNextScheduledCron(staticTimeMillis, new LabelCron("cron1", "", LabelAction.REPLACE, ""));
        Assert.assertEquals(null, res);
    }

    @Test
    public void testGetNextScheduledCronOverMin() {
        LabelCron res = periodicWork.getNextScheduledCron(staticTimeMillis, new LabelCron("cron1", "H/5 * * * *", LabelAction.REPLACE, "t"));
        Assert.assertEquals(null, res);
    }

    @Test
    public void testGetNextScheduledCronExactPast() {
        // can't actually do year, so this is 1 year forward
        LabelCron res = periodicWork.getNextScheduledCron(staticTimeMillis, new LabelCron("cron1", "30 07 16 12 *", LabelAction.REPLACE, "t"));
        Assert.assertEquals(null, res);
    }

    @Test
    public void testGetNextScheduledCronExact() {
        LabelCron res = periodicWork.getNextScheduledCron(staticTimeMillis, new LabelCron("cron1", "32 08 16 12 *", LabelAction.REPLACE, "t"));
        Assert.assertEquals(null, res);
    }

    @Test
    public void testGetNextScheduledCronMultiDiff() {
        LabelCron labelCron1 = new LabelCron("labelCron1", "H/5 * * * *", LabelAction.REPLACE, "t");
        LabelCron labelCron2 = new LabelCron("labelCron2", "* * * * *", LabelAction.REPLACE, "t");
        LabelCron labelCron3 = new LabelCron("labelCron3", "H/15 * * * *", LabelAction.REPLACE, "t");
        LabelCron res = periodicWork.getNextScheduledCron(staticTimeMillis, labelCron1, labelCron2, labelCron3);
        Assert.assertEquals(labelCron2, res);
    }

    @Test
    public void testGetNextScheduledCronMultiSame() {
        LabelCron labelCron1 = new LabelCron("labelCron1", "* * * * *", LabelAction.REPLACE, "t");
        LabelCron labelCron2 = new LabelCron("labelCron2", "* * * * *", LabelAction.REPLACE, "t");
        LabelCron res = periodicWork.getNextScheduledCron(staticTimeMillis, labelCron1, labelCron2);
        Assert.assertEquals(labelCron1, res);
    }

    @Test
    public void testGetNextScheduledCronMultiDiffLive() {
        int currentMin = Calendar.getInstance().get(Calendar.MINUTE);
        final long currentTime = System.currentTimeMillis();
        LabelCron labelCron1 = new LabelCron("labelCron1", "H/" + (currentMin + 5) + " * * * *", LabelAction.REPLACE, "t");
        LabelCron labelCron2 = new LabelCron("labelCron2", "* * * * *", LabelAction.REPLACE, "t");
        LabelCron res = periodicWork.getNextScheduledCron(currentTime, labelCron1, labelCron2);
        Assert.assertEquals(labelCron2, res);
    }

    @Test
    public void testGetNextScheduledCronMultiSameLive() {
        final long currentTime = System.currentTimeMillis();
        LabelCron labelCron1 = new LabelCron("labelCron1", "* * * * *", LabelAction.REPLACE, "t");
        LabelCron labelCron2 = new LabelCron("labelCron2", "* * * * *", LabelAction.REPLACE, "t");
        LabelCron res = periodicWork.getNextScheduledCron(currentTime, labelCron1, labelCron2);
        Assert.assertEquals(labelCron1, res);
    }

    /**
     * Test is ignored as it testing for a known issue in J/H <= 1.395
     */
    @Test
    @Ignore
    public void testGetWeekendScheduledCron() {
        final Calendar cal = Calendar.getInstance();
        cal.set(2011, 0, 16, 0, 0, 0);
        final long testTime = cal.getTimeInMillis();
        LabelCron labelCron1 = new LabelCron("labelCron1", "0 23 * * 0", LabelAction.REPLACE, "t");
        LabelCron labelCron2 = new LabelCron("labelCron2", "0 23 * * 1-5", LabelAction.REPLACE, "t");
        periodicWork.getNextScheduledCron(testTime, labelCron1, labelCron2);
    }

    /**
     * Test is ignored as it testing for a known issue in J/H <= 1.395
     *
     * @throws ANTLRException
     */
    @Test
    @Ignore
    public void testHudsonCeil() throws ANTLRException {
        final Calendar cal = Calendar.getInstance();
        cal.set(2011, 0, 16, 0, 0, 0); // Sunday, Jan 16th 2011, 00:00
        final String cronStr = "0 23 * * 1-5"; // execute on weekdays @23:00

        final CronTab cron = new CronTab(cronStr);
        final Calendar next = cron.ceil(cal);

        final Calendar expectedDate = Calendar.getInstance();
        expectedDate.set(2011, 0, 17, 23, 0, 0); // Expected next: Monday, Jan 17th 2011, 23:00
        Assert.assertEquals(expectedDate.get(Calendar.HOUR), next.get(Calendar.HOUR));
        Assert.assertEquals(expectedDate.get(Calendar.MINUTE), next.get(Calendar.MINUTE));
        Assert.assertEquals(expectedDate.get(Calendar.YEAR), next.get(Calendar.YEAR));
        Assert.assertEquals(expectedDate.get(Calendar.MONTH), next.get(Calendar.MONTH));
        Assert.assertEquals(expectedDate.get(Calendar.DAY_OF_MONTH), next.get(Calendar.DAY_OF_MONTH)); // FAILS: is Monday,
        // Jan 10th, 23:00
    }
}
