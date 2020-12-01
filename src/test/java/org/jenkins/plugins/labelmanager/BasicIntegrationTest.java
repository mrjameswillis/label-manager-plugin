package org.jenkins.plugins.labelmanager;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockBuilder;
import org.jvnet.hudson.test.TestExtension;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;

public class BasicIntegrationTest {

	@Rule
	public JenkinsRule j = new JenkinsRule();

	@TestExtension
	public static class PrinterBuilder extends MockBuilder {

		public PrinterBuilder() {
			super(Result.SUCCESS);
		}

		@Override
		public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
			listener.getLogger().println("resourceNameVar: " + build.getEnvironment(listener).get("resourceNameVar"));
			return true;
		}
		
	}

}
