package com.prezi.pride.cli.commands;

import com.prezi.pride.Pride;
import com.prezi.pride.PrideException;
import com.prezi.pride.RuntimeConfiguration;
import com.prezi.pride.cli.commands.actions.InitAction;
import com.prezi.pride.cli.commands.actions.InitActionBase;
import com.prezi.pride.cli.commands.actions.InitActionFromImportedConfig;
import io.airlift.airline.Command;
import io.airlift.airline.Option;

import java.io.File;

import static com.prezi.pride.cli.Configurations.GRADLE_WRAPPER;

@Command(name = "init", description = "Initialize pride")
public class InitCommand extends AbstractConfiguredCommand {

	@Option(name = {"-f", "--force"},
			description = "Force initialization of a pride, even if one already exists")
	private boolean explicitForce;

	@Option(name = "--with-wrapper",
			description = "Add a Gradle wrapper")
	private boolean explicitWithWrapper;

	@Option(name = "--no-wrapper",
			description = "Do not add Gradle wrapper")
	private boolean explicitNoWrapper;

	@Option(name = "--no-add-existing",
			description = "Do not add existing modules in the pride directory to the pride")
	private boolean explicitNoAddExisting;

	@Option(name = "--ignore-config",
			description = "Ignore existing pride's configuration (to be used with --force)")
	private boolean explicitIgnoreConfig;

	@Option(name = "--import",
			title = "file or URL",
			description = "Import configuration and modules from configuration file exported by 'pride export' (use '-' to read from standard input)")
	private String explicitImport;

	@Deprecated
	@Option(name = "--from-config",
			hidden = true)
	private String explicitFromConfig;

	@Option(name = {"-c", "--use-repo-cache"},
			description = "Use local repo cache (when adding modules from existing configuration)")
	private boolean explicitUseRepoCache;

	@Option(name = {"--no-repo-cache"},
			description = "Do not use local repo cache (when adding modules from existing configuration)")
	private boolean explicitNoRepoCache;

	@Option(name = {"-r", "--recursive"},
			description = "Update sub-modules recursively (when adding modules from existing configuration)")
	private Boolean explicitRecursive;

	@Override
	protected void executeWithConfiguration(RuntimeConfiguration globalConfig) throws Exception {
		if (!explicitForce) {
			File parentPrideDirectory = Pride.findPrideDirectory(getPrideDirectory());
			if (parentPrideDirectory != null) {
				throw new PrideException("An existing pride has been found in " + parentPrideDirectory);
			}
		}
		boolean addWrapper = globalConfig.override(GRADLE_WRAPPER, explicitWithWrapper, explicitNoWrapper);

		InitActionBase initAction;
		String configToImport;
		//noinspection deprecation
		if (explicitFromConfig != null) {
			logger.warn("The --from-config option is deprecated, and will be removed in a future release. Please use --import instead.");
			//noinspection deprecation
			configToImport = explicitFromConfig;
		} else {
			configToImport = explicitImport;
		}
		if (configToImport == null) {
			initAction = InitAction.create(getPrideDirectory(), globalConfig, getVcsManager(), explicitForce, !explicitNoAddExisting, explicitIgnoreConfig);
		} else {
			initAction = InitActionFromImportedConfig.create(getPrideDirectory(), globalConfig, getVcsManager(), configToImport, explicitUseRepoCache, explicitNoRepoCache, explicitRecursive);
		}
		initAction.createPride(addWrapper, isVerbose());
	}
}
