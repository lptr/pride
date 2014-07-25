package com.prezi.gradle.pride.cli.commands;

import com.prezi.gradle.pride.Pride;
import com.prezi.gradle.pride.PrideException;
import com.prezi.gradle.pride.cli.PrideInitializer;
import com.prezi.gradle.pride.vcs.Vcs;
import io.airlift.command.Command;
import io.airlift.command.Option;
import org.apache.commons.configuration.Configuration;

import java.io.File;
import java.io.FileFilter;

@Command(name = "init", description = "Initialize pride")
public class InitCommand extends AbstractConfiguredCommand {

	@Option(name = {"-f", "--force"},
			description = "Force initialization of a pride, even if one already exists")
	private boolean overwrite;

	@Option(name = "--no-add-existing",
			description = "Do not add existing modules in the pride directory to the pride")
	private boolean explicitNoAddExisting;

	@Option(name = "--gradle-version",
			title = "version",
			description = "Use specified Gradle version")
	private String explicitGradleVersion;

	@Override
	protected int executeWithConfiguration(Configuration globalConfig) throws Exception {
		boolean prideExistsAlready = Pride.containsPride(getPrideDirectory());
		if (!overwrite && prideExistsAlready) {
			throw new PrideException("A pride already exists in " + getPrideDirectory());
		}

		Configuration config = globalConfig;
		if (prideExistsAlready) {
			try {
				Pride pride = Pride.getPride(getPrideDirectory(), globalConfig, getVcsManager());
				config = pride.getConfiguration();
			} catch (Exception ex) {
				logger.warn("Could not load existing pride, ignoring existing configuration");
				logger.debug("Exception was", ex);
			}
		}
		final Pride pride = PrideInitializer.create(getPrideDirectory(), globalConfig, getVcsManager());

		if (!explicitNoAddExisting) {
			logger.debug("Adding existing modules");
			boolean addedAny = false;
			for (File dir : getPrideDirectory().listFiles(new FileFilter() {
				@Override
				public boolean accept(File path) {
					return path.isDirectory();
				}
			})) {
				if (Pride.isValidModuleDirectory(dir)) {
					Vcs vcs = getVcsManager().findSupportingVcs(dir, config);
					logger.info("Adding existing " + vcs.getType() + " module in " + dir);
					pride.addModule(dir.getName(), vcs);
					addedAny = true;
				}
			}
			if (addedAny) {
				pride.save();
				PrideInitializer.reinitialize(pride);
			}
		}
		return 0;
	}
}
