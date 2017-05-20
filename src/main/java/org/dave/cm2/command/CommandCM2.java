package org.dave.cm2.command;

public class CommandCM2 extends CommandMenu {

    @Override
    public void initEntries() {
        this.addSubcommand(new CommandSchema());
        this.addSubcommand(new CommandRecipe());
        this.addSubcommand(new CommandEntitySize());
    }

    @Override
    public String getName() {
        return "cm2";
    }
}
