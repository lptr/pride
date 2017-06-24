package com.prezi.pride.cli.commands.actions;

import com.prezi.pride.Pride;
import com.prezi.pride.cli.gradle.GradleConnectorManager;
import com.prezi.pride.cli.gradle.GradleProjectExecution;
import org.gradle.tooling.ProjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class RefreshDependenciesAction {
	private static final Logger logger = LoggerFactory.getLogger(RefreshDependenciesAction.class);

	public void refreshDependencies(Pride pride) {
		logger.info("Refreshing dependencies");
		GradleConnectorManager gradleManager = new GradleConnectorManager(pride.getConfiguration());
		gradleManager.executeInProject(pride.getRootDirectory(), new GradleProjectExecution<Void, RuntimeException>() {
			@Override
			public Void execute(File projectDirectory, ProjectConnection connection) {
				connection.newBuild()
						.forTasks("help")
						.withArguments("--refresh-dependencies")
						.run();
				return null;
			}
		});
	}
}
