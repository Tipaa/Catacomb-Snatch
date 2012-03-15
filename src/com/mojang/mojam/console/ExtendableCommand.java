package com.mojang.mojam.console;

import com.mojang.mojam.console.Console.Command;

public abstract class ExtendableCommand extends Console.Command implements ICommand {

    Console console;
    public ExtendableCommand(String name, int numberOfArgs, String helpMessage, Console cons) {
	super(name, numberOfArgs, helpMessage);
	console = cons;
    }
    
    public void log(String s)
    {
	console.log(s);
    }

    public abstract void doCommand(String[] args);

}
