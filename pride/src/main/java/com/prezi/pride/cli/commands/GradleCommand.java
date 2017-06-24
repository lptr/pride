package com.prezi.pride.cli.commands;

import com.prezi.pride.Pride;
import com.prezi.pride.cli.gradle.GradleConnectorManager;
import com.prezi.pride.cli.gradle.GradleProjectExecution;
import io.airlift.airline.Arguments;
import io.airlift.airline.Command;
import org.gradle.tooling.ProjectConnection;

import java.io.File;
import java.util.List;

@Command(name = "gradle", description = "Run Gradle from the root of the pride")
public class GradleCommand extends AbstractPrideCommand {
	@Arguments
	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	private List<String> arguments;

	@Override
	public void executeInPride(Pride pride) throws Exception {
		new GradleConnectorManager(pride.getConfiguration()).executeInProject(pride.getRootDirectory(), new GradleProjectExecution<Void, RuntimeException>() {
			@Override
			public Void execute(File directory, ProjectConnection connection) {
				connection.newBuild()
						.withArguments(arguments.toArray(new String[arguments.size()]))
						.run();
				return null;
			}
		});
	}
}
