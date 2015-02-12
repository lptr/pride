package com.prezi.pride.cli;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.prezi.pride.Module;
import com.prezi.pride.Pride;
import com.prezi.pride.PrideException;
import com.prezi.pride.RuntimeConfiguration;
import com.prezi.pride.cli.gradle.GradleConnectorManager;
import com.prezi.pride.cli.model.ProjectModelAccessor;
import com.prezi.pride.internal.LoggedNamedProgressAction;
import com.prezi.pride.internal.ProgressUtils;
import com.prezi.pride.projectmodel.PrideProjectModel;
import com.prezi.pride.vcs.VcsManager;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;

public class PrideInitializer {

	private static final Logger logger = LoggerFactory.getLogger(PrideInitializer.class);
	private static final String DO_NOT_MODIFY_WARNING =
			"//\n" +
			"// DO NOT MODIFY -- This file is generated by Pride, and will be\n" +
			"// overwritten whenever the pride itself is changed.\n//\n";
	private final GradleConnectorManager gradleConnectorManager;
	private final boolean verbose;

	public PrideInitializer(GradleConnectorManager gradleConnectorManager, boolean verbose) {
		this.gradleConnectorManager = gradleConnectorManager;
		this.verbose = verbose;
	}

	public Pride create(File prideDirectory, RuntimeConfiguration globalConfig, Configuration prideConfig, VcsManager vcsManager) throws IOException, ConfigurationException {
		logger.info("Initializing {}", prideDirectory);
		FileUtils.forceMkdir(prideDirectory);

		File configDirectory = Pride.getPrideConfigDirectory(prideDirectory);
		FileUtils.deleteDirectory(configDirectory);
		FileUtils.forceMkdir(configDirectory);
		FileUtils.write(Pride.getPrideVersionFile(configDirectory), "0\n");

		// Create config file
		File configFile = Pride.getPrideConfigFile(configDirectory);
		PropertiesConfiguration prideFileConfig = new PropertiesConfiguration(configFile);
		boolean prideConfigModified = false;
		for (String key : Iterators.toArray(prideConfig.getKeys(), String.class)) {
			// Skip modules
			if (key.startsWith("modules.")) {
				continue;
			}
			prideFileConfig.setProperty(key, prideConfig.getProperty(key));
			prideConfigModified = true;
		}
		// Override Gradle details
		if (gradleConnectorManager.setGradleConfiguration(prideFileConfig)) {
			prideConfigModified = true;
		}
		if (prideConfigModified) {
			prideFileConfig.save();
		}

		Pride pride = new Pride(prideDirectory, globalConfig, prideFileConfig, vcsManager);
		reinitialize(pride);
		return pride;
	}

	public void reinitialize(Pride pride) {
		try {
			File buildFile = pride.getGradleBuildFile();
			FileUtils.deleteQuietly(buildFile);
			FileUtils.write(buildFile, DO_NOT_MODIFY_WARNING);
			FileOutputStream buildOut = new FileOutputStream(buildFile, true);
			try {
				IOUtils.copy(PrideInitializer.class.getResourceAsStream("/build.gradle"), buildOut);
			} finally {
				buildOut.close();
			}

			final ProjectModelAccessor modelAccessor = ProjectModelAccessor.create(gradleConnectorManager, verbose);
			final Map<File, PrideProjectModel> rootProjects = Maps.newLinkedHashMap();
			ProgressUtils.execute(pride, pride.getModules(), new LoggedNamedProgressAction<Module>("Initializing module") {
				@Override
				public void execute(Pride pride, Module module) {
					File moduleDirectory = new File(pride.getRootDirectory(), module.getName());
					if (Pride.isValidModuleDirectory(moduleDirectory)) {
						PrideProjectModel rootProject = modelAccessor.getRootProjectModel(moduleDirectory);
						rootProjects.put(moduleDirectory, rootProject);
					}
				}

				@Override
				public void execute(Pride pride, Module item, int index, int count) throws IOException {
					super.execute(pride, item, index, count);
					if (index < count - 1) {
						logger.info("");
					}
				}
			});

			createSettingsFile(pride, rootProjects);
		} catch (Exception ex) {
			throw new PrideException("There was a problem during the initialization of the pride. Fix the errors above, and try again with\n\n\tpride init --force", ex);
		}
	}

	private void createSettingsFile(Pride pride, Map<File, PrideProjectModel> rootProjects) throws IOException {
		File settingsFile = pride.getGradleSettingsFile();
		FileUtils.deleteQuietly(settingsFile);
		FileUtils.write(settingsFile, DO_NOT_MODIFY_WARNING);
		for (Map.Entry<File, PrideProjectModel> entry : rootProjects.entrySet()) {
			File moduleDirectory = entry.getKey();
			PrideProjectModel rootProject = entry.getValue();

			// Merge settings
			String relativePath = pride.getRootDirectory().toURI().relativize(moduleDirectory.toURI()).toString();
			FileUtils.write(settingsFile, "\n// Settings from project in directory /" + relativePath + "\n\n", true);
			// Write the root project
			FileUtils.write(settingsFile, "include \'" + rootProject.getName() + "\'\n", true);
			FileUtils.write(settingsFile, "project(\':" + rootProject.getName() + "\').projectDir = file(\'" + moduleDirectory.getName() + "\')\n", true);
			writeSettingsForChildren(pride.getRootDirectory(), settingsFile, rootProject.getName(), rootProject.getChildren());
		}
	}

	private void writeSettingsForChildren(File prideRootDir, File settingsFile, String rootProjectName, Set<PrideProjectModel> children) throws IOException {
		for (PrideProjectModel child : children) {
			FileUtils.write(settingsFile, "include \'" + rootProjectName + child.getPath() + "\'\n", true);
			String childProjectRelativePath = URI.create(prideRootDir.getCanonicalPath()).relativize(URI.create(child.getProjectDir())).toString();
			FileUtils.write(settingsFile, "project(\':" + rootProjectName + child.getPath() + "\').projectDir = file(\'" + childProjectRelativePath + "\')\n", true);
			writeSettingsForChildren(prideRootDir, settingsFile, rootProjectName, child.getChildren());
		}
	}
}
