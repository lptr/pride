package com.prezi.pride.cli.commands.actions;

import com.prezi.pride.Pride;
import com.prezi.pride.RuntimeConfiguration;
import com.prezi.pride.cli.PrideInitializer;
import com.prezi.pride.cli.gradle.GradleConnectorManager;
import com.prezi.pride.cli.gradle.GradleProjectExecution;
import com.prezi.pride.vcs.VcsManager;
import org.apache.commons.configuration.Configuration;
import org.gradle.tooling.ProjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public abstract class InitActionBase {
	private static final Logger logger = LoggerFactory.getLogger(InitActionBase.class);

	private final File prideDirectory;
	protected final RuntimeConfiguration globalConfig;
	protected final Configuration prideConfig;
	protected final VcsManager vcsManager;

	protected InitActionBase(File prideDirectory, RuntimeConfiguration globalConfig, Configuration prideConfig, VcsManager vcsManager) {
		this.prideDirectory = prideDirectory;
		this.globalConfig = globalConfig;
		this.prideConfig = prideConfig;
		this.vcsManager = vcsManager;
	}

	public final void createPride(boolean addWrapper, boolean verbose) throws Exception {
		// Make sure we take the local config into account when choosing the Gradle installation
		RuntimeConfiguration configForGradle = globalConfig.withConfiguration(prideConfig);

		if (addWrapper) {
			logger.info("Adding Gradle wrapper");
			GradleConnectorManager gradleConnectorManager = new GradleConnectorManager(configForGradle);
			gradleConnectorManager.executeInProject(prideDirectory, new GradleProjectExecution<Void, RuntimeException>() {
				@Override
				public Void execute(File projectDirectory, ProjectConnection connection) {
					connection.newBuild()
							.forTasks("wrapper")
							.run();
					return null;
				}
			});
		}

		// Create the pride
		PrideInitializer prideInitializer = new PrideInitializer(verbose);
		Pride pride = prideInitializer.create(prideDirectory, globalConfig, prideConfig, vcsManager);
		initPride(prideInitializer, pride, verbose);
	}

	abstract protected void initPride(PrideInitializer prideInitializer, Pride pride, boolean verbose) throws Exception;
}
