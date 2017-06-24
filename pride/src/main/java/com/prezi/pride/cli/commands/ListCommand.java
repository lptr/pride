package com.prezi.pride.cli.commands;

import com.google.common.base.Strings;
import com.prezi.pride.Module;
import com.prezi.pride.Pride;
import com.prezi.pride.vcs.VcsStatus;
import io.airlift.airline.Command;
import io.airlift.airline.Option;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

@Command(name = "list", description = "List modules in a pride")
public class ListCommand extends AbstractFilteredPrideCommand {

	@Option(name = {"-I", "--include"},
			title = "regex",
			description = "Execute the command on module (can be specified multiple times)")
	private List<String> includeModules;

	@Option(name = {"-s", "--short"},
			description = "Show only module names")
	private boolean explicitShort;

	@Override
	protected void executeInModules(Pride pride, Collection<Module> modules) throws Exception {
		LineFormatter formatter;
		if (explicitShort) {
			formatter = new NamesOnlyFormatter();
		} else {
			formatter = new StatusFormatter(pride);
		}
		for (Module module : modules) {
			logger.info("{}", formatter.formatModule(module));
		}
	}

	@Override
	protected Collection<String> getIncludeModules() {
		return includeModules;
	}

	private interface LineFormatter {
		String formatModule(Module module) throws Exception;
	}

	private static class NamesOnlyFormatter implements LineFormatter {

		@Override
		public String formatModule(Module module) {
			return module.getName();
		}
	}

	private static class StatusFormatter implements LineFormatter {

		private final Pride pride;
		private int moduleMaxLength;
		private int branchMaxLength;

		private StatusFormatter(Pride pride) throws IOException {
			this.pride = pride;

			Collection<Module> modules = pride.getModules();
			for (Module module : modules) {
				String name = module.getName();
				this.moduleMaxLength = Math.max(this.moduleMaxLength, name.length());

				String branch = module.getVcs().getSupport().getStatus(pride.getModuleDirectory(name)).getBranch();
				if (!Strings.isNullOrEmpty(branch)) {
					this.branchMaxLength = Math.max(this.branchMaxLength, branch.length());
				}
			}
		}

		@Override
		public String formatModule(Module module) throws Exception {
			File moduleDirectory = pride.getModuleDirectory(module.getName());
			// MM module-name (git)
			VcsStatus status = module.getVcs().getSupport().getStatus(moduleDirectory);
			String branch = status.getBranch();
			StringBuilder line = new StringBuilder();

			line.append(status.hasUnpublishedChanges() ? 'M' : ' ');
			line.append(status.hasUncommittedChanges() ? 'M' : ' ');
			line.append(' ').append(module.getName());

			line.append(Strings.repeat(" ", this.moduleMaxLength + 2 - module.getName().length()));

			if (!Strings.isNullOrEmpty(branch)) {
				line.append(branch);
				line.append(Strings.repeat(" ", this.branchMaxLength + 2 - branch.length()));
				line.append('@');
			}
			line.append(status.getRevision());
			line.append(" (").append(module.getVcs().getType()).append(")");
			return line.toString();
		}
	}
}
