package com.prezi.gradle.pride

import org.gradle.tooling.GradleConnector
import org.gradle.tooling.model.gradle.GradleBuild

/**
 * Created by lptr on 31/03/14.
 */
class SessionInitializer {
	public static final String SETTINGS_GRADLE = "settings.gradle"
	public static final String BUILD_GRADLE = "build.gradle"

	public static void initializeSession(File sessionDirectory, boolean overwrite) {
		System.out.println("Starting Gradle connector")
		def connector = GradleConnector.newConnector()
		System.out.println("Initializing ${sessionDirectory}")

		def settingsFile = new File(sessionDirectory, SETTINGS_GRADLE)
		def buildFile = new File(sessionDirectory, BUILD_GRADLE)

		if (!overwrite && (settingsFile.exists() || buildFile.exists())) {
			throw new IllegalStateException("A session already exists in ${sessionDirectory}")
		}

		sessionDirectory.mkdirs()
		settingsFile.delete()
		buildFile.delete()

		sessionDirectory.eachDir { dir ->
			if (isValidProject(dir)) {
				def connection = connector.forProjectDirectory(dir).connect()
				try {
					def relativePath = sessionDirectory.toURI().relativize(dir.toURI()).toString()
					settingsFile << "\n// Project from directory /${relativePath}\n"
					// Load the model for the build
					GradleBuild build = connection.getModel(GradleBuild)
					build.projects.each { prj ->
						if (prj == build.rootProject) {
							settingsFile << "include '$build.rootProject.name'\n"
							settingsFile << "project(':$build.rootProject.name').projectDir = file('$dir.name')\n"
						} else {
							settingsFile << "include '$build.rootProject.name$prj.path'\n"
						}
					}
				} finally {
					// Clean up
					connection.close()
				}
			}
		}

		buildFile << getClass().getResourceAsStream("/build.gradle")
	}

	private static boolean isValidProject(File dir) {
		System.out.println("Scanning ${dir}")
		return !dir.name.startsWith(".") &&
				dir.list().contains(BUILD_GRADLE) ||
				dir.list().contains(SETTINGS_GRADLE)
	}
}