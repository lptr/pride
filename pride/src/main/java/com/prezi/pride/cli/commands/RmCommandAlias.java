package com.prezi.pride.cli.commands;

import io.airlift.airline.Command;

@Command(name = "rm", hidden = true, description = "Remove modules from a pride")
public class RmCommandAlias extends RemoveCommand {
}
