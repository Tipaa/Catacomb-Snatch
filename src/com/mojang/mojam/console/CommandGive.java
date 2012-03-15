package com.mojang.mojam.console;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import com.mojang.mojam.MojamComponent;
import com.mojang.mojam.entity.Player;
import com.mojang.mojam.entity.mob.Mob;
import com.mojang.mojam.entity.weapon.Cannon;
import com.mojang.mojam.entity.weapon.ElephantGun;
import com.mojang.mojam.entity.weapon.Flamethrower;
import com.mojang.mojam.entity.weapon.IWeapon;
import com.mojang.mojam.entity.weapon.Machete;
import com.mojang.mojam.entity.weapon.Melee;
import com.mojang.mojam.entity.weapon.Raygun;
import com.mojang.mojam.entity.weapon.Rifle;
import com.mojang.mojam.entity.weapon.Shotgun;
import com.mojang.mojam.entity.weapon.VenomShooter;

public class CommandGive extends ExtendableCommand implements ICommand {

    public CommandGive(String name, int numberOfArgs, String helpMessage,
	    Console console) {
	super(name, numberOfArgs, helpMessage, console);
	this.console = console;
    }

    public static List<String> commands = new ArrayList<String>();
    public static List<String> texts = new ArrayList<String>();
    public static List<Class> weapon = new ArrayList<Class>();
    public Console console;
    private static Object hook = addAll();

    public static void addWeapon(String comm, String text, Class wep) {
	commands.add(comm);
	texts.add(text);
	weapon.add(wep);
    }

    public static Object addAll() {
	addWeapon("shotgun", "Giving player a shotgun", Shotgun.class);
	addWeapon("rifle", "Giving player a rifle", Rifle.class);
	addWeapon("venom", "Giving player a Venomshooter", VenomShooter.class);
	addWeapon("elephant", "Giving player an elephant gun",
		ElephantGun.class);
	addWeapon("cannon", "Giving player a cannon", Cannon.class);
	addWeapon("fist", "Giving player a lesson in boxing", Melee.class);
	addWeapon("machete", "Giving player a machete", Machete.class);
	addWeapon("raygun", "Giving player a raygun", Raygun.class);
	addWeapon("flamethrower", "Giving player a flamethrower",
		Flamethrower.class);
	return null;
    }

    @Override
    public void doCommand(String[] args) {
	args[0].trim();
	Player player = MojamComponent.instance.player;
	int i = commands.indexOf(args[0]);
	if (i > -1) {
	    log(texts.get(i));
	    try {
		if (!player.weaponInventory.add((IWeapon) weapon.get(i)
			.getDeclaredConstructor(Mob.class).newInstance(player))) {
		    log("You already have this item.");
		}
		return;
	    } catch (InstantiationException e) {
		e.printStackTrace();
	    } catch (IllegalAccessException e) {
		e.printStackTrace();
	    } catch (IllegalArgumentException e) {
		e.printStackTrace();
	    } catch (InvocationTargetException e) {
		e.printStackTrace();
	    } catch (NoSuchMethodException e) {
		e.printStackTrace();
	    } catch (SecurityException e) {
		e.printStackTrace();
	    }
	} else if (args[0].toLowerCase().equals("help")) {
	    log("Options:");
	    for (int j = 0; j < weapon.size(); j++) {
		log(">" + commands.get(j) + " ("
			+ weapon.get(j).getSimpleName() + ")");
	    }
	    log("Or you can use a numerical value to receive money.");
	    return;
	}
	try {
	    player.score += Integer.parseInt(args[0]);
	} catch (NumberFormatException e) {
	    log("Incorrect command parameter: " + args[0]);
	}
    }

}
