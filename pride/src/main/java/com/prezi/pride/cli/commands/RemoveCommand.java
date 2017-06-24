package com.prezi.pride.cli.commands;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.prezi.pride.Module;
import com.prezi.pride.Pride;
import com.prezi.pride.PrideException;
import com.prezi.pride.cli.PrideInitializer;
import com.prezi.pride.internal.LoggedNamedProgressAction;
import com.prezi.pride.internal.ProgressUtils;
import io.airlift.airline.Arguments;
import io.airlift.airline.Command;
import io.airlift.airline.Option;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

@Command(name = "remove", description = "Remove modules from a pride")
public class RemoveCommand extends AbstractFilteredPrideCommand {

	@Option(name = {"-f", "--force"},
			description = "Remove modules even if there are local changes")
	private boolean force;

	@Arguments(description = "Modules to remove from the pride")
	private List<String> includeModules;

	@Override
	protected void executeInModules(final Pride pride, Collection<Module> modules) throws Exception {
		// Check if anything exists already
		if (!force) {
			Collection<Module> changedModules = Collections2.filter(modules, new Predicate<Module>() {
				@Override
				public boolean apply(Module module) {
					File moduleDir = pride.getModuleDirectory(module.getName());
					try {
						return module.getVcs().getSupport().hasChanges(moduleDir);
					} catch (RuntimeException e) {
						throw e;
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			});

			if (!changedModules.isEmpty()) {
				throw new PrideException("These modules have changes: " + Joiner.on(", ").join(changedModules));
			}
		}

		// Remove modules
		final List<String> failedModules = Lists.newArrayList();
		ProgressUtils.execute(pride, modules, new LoggedNamedProgressAction<Module>("Removing") {
			@Override
			protected void execute(Pride pride, Module module) throws IOException {
				String moduleName = module.getName();
				try {
					pride.removeModule(moduleName);
				} catch (Exception ex) {
					logger.warn("Could not remove module {}: {}", moduleName, ex);
					logger.debug("Exception while removing module {}", moduleName, ex);
					failedModules.add(moduleName);
				}
			}
		});
		pride.save();

		// Re-initialize pride
		new PrideInitializer(isVerbose()).reinitialize(pride);

		// Show error if not all modules could be removed
		if (!failedModules.isEmpty()) {
			throw new PrideException("Could not remove the following modules completely:\n\n\t* " + Joiner.on("\n\t* ").join(failedModules));
		}
	}

	@Override
	protected Collection<String> getIncludeModules() {
		return includeModules;
	}

	@Override
	protected void handleNoFilterSpecified() {
		throw new PrideException("No modules to remove have been specified");
	}

	@Override
	protected void handleNoMatchingModulesFound() {
		logger.warn("No matching modules found");
	}
}
