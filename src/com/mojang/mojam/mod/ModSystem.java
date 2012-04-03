package com.mojang.mojam.mod;

import static com.mojang.mojam.mod.ModSystem.mojam;
import static com.mojang.mojam.mod.ModSystem.print;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.lwjgl.opengl.XRandR.Screen;

import com.mojang.mojam.InputHandler;
import com.mojang.mojam.Keys;
import com.mojang.mojam.Keys.Key;
import com.mojang.mojam.MojamComponent;
import com.mojang.mojam.Options;
import com.mojang.mojam.console.Console;
import com.mojang.mojam.entity.Entity;
import com.mojang.mojam.entity.weapon.IWeapon;
import com.mojang.mojam.gui.TitleMenu;
import com.mojang.mojam.gui.components.Font;
import com.mojang.mojam.level.Level;
import com.mojang.mojam.level.gamemode.GameMode;
import com.mojang.mojam.network.kryo.Network;
import com.mojang.mojam.network.kryo.Network.ChangeKeyMessage;
import com.mojang.mojam.screen.AbstractBitmap;
import com.mojang.mojam.screen.AbstractScreen;

public final class ModSystem {

    private static boolean init = false;
    public static File modDir;
    public static File modsFolder;
    public static List<IMod> modList;
    public static List<ScriptEngine> scriptList;
    static MojamComponent mojam;
    private static Level level;
    private static InputHandler inputHandler;
    private static Keys keys;
    public static Map<Integer, Class> spawnList;
    private static ScriptEngineManager lang;
    public static boolean isJar;
    public static boolean isDebug;
    public static Console console;

    /**
     * Just in case
     */
    public static void reload() {
	init = false;
	modDir = null;
	modsFolder = null;
	modList.clear();
	scriptList.clear();
	level = null;
	inputHandler = null;
	keys = null;
	spawnList.clear();
	lang = null;
	isJar = false;
	isDebug = false;
	console = null;
	init(MojamComponent.instance);
    }

    /**
     * Entry point for the Mod System
     * 
     * @param m
     *            The instance of MojamComponent
     */
    public static void init(MojamComponent m) {
	if (init)
	    return;
	init = true;
	mojam = m;
	console = mojam.console;
	modList = new ArrayList<IMod>();
	scriptList = new ArrayList<ScriptEngine>();
	keys = new Keys();
	spawnList = new HashMap<Integer, Class>();
	lang = new ScriptEngineManager();
	keys.getAll().removeAll(keys.getAll());
	try {
	    modDir = new File(new File(MojamComponent.class
		    .getProtectionDomain().getCodeSource().getLocation()
		    .toURI()), "/script");
	    modsFolder = new File(mojam.getMojamDir(), "/mods/");
	    isJar = modDir.getAbsolutePath().endsWith(".jar");
	    isDebug = !isJar;
	} catch (URISyntaxException e1) {
	    e1.printStackTrace();
	}

	print("ModSystem starting up...");
	addMod(ModSystem.class.getClassLoader(), "SnatchContent.class");
	inputHandler = (InputHandler) reflectField(mojam, "inputHandler");
	try {
	    SubscriptionsHandler.run();
	    readFromModFolder(modDir);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    /**
     * Loads all scripts found in the mods folder into the game.
     */
    private static void readFromModsFolder() {
	File[] files = modsFolder.listFiles();
	boolean valid;
	for (File f : files) {
	    valid = true;
	    if (valid)
		addScript(f.getAbsolutePath());
	}
    }

    /**
     * Loads Java mods from a given folder
     * 
     * @param file
     *            The folder to search
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws SecurityException
     * @throws NoSuchMethodException
     */
    private static void readFromModFolder(File file) throws IOException,
	    IllegalArgumentException, IllegalAccessException,
	    InvocationTargetException, SecurityException, NoSuchMethodException {

	ClassLoader classloader = (MojamComponent.class).getClassLoader();
	Method method = (URLClassLoader.class).getDeclaredMethod("addURL",
		new Class[] { URL.class });
	method.setAccessible(true);
	if (!file.isDirectory()) {
	    throw new IllegalArgumentException("Folder must be a Directory.");
	}
	File afile[] = file.listFiles();
	Arrays.sort(afile);
	if (classloader instanceof URLClassLoader) {
	    for (int i = 0; i < afile.length; i++) {
		File file1 = afile[i];
		if (file1.isDirectory()
			|| file1.isFile()
			&& (file1.getName().endsWith(".jar") || file1.getName()
				.endsWith(".zip"))) {
		    method.invoke(classloader, new Object[] { file1.toURI()
			    .toURL() });
		}
	    }
	}
	for (int j = 0; j < afile.length; j++) {
	    File file2 = afile[j];
	    if (addScript(file2.getAbsolutePath()) != null) {
		continue;
	    }
	    if (file2.isDirectory()
		    || file2.isFile()
		    && (file2.getName().endsWith(".jar") || file2.getName()
			    .endsWith(".zip"))) {
		if (file2.isFile()) {
		    FileInputStream fileinputstream = new FileInputStream(file2);
		    ZipInputStream zipinputstream = new ZipInputStream(
			    fileinputstream);
		    Object obj = null;
		    do {
			ZipEntry zipentry = zipinputstream.getNextEntry();
			if (zipentry == null) {
			    break;
			}
			String s1 = zipentry.getName();
			if (!zipentry.isDirectory() && s1.startsWith("mod_")
				&& s1.endsWith(".class")) {
			    addMod(classloader, s1);
			} else if (!zipentry.isDirectory()
				&& s1.startsWith("mod_") && !s1.contains(".MF")) {
			    addScript(zipentry);
			}
		    } while (true);
		    zipinputstream.close();
		    fileinputstream.close();
		} else if (file2.isDirectory()) {
		    Package package1 = (com.mojang.mojam.MojamComponent.class)
			    .getPackage();
		    if (package1 != null) {
			String s = package1.getName().replace('.',
				File.separatorChar);
			file2 = new File(file2, s);
		    }
		    File afile1[] = file2.listFiles();
		    if (afile1 != null) {
			for (int k = 0; k < afile1.length; k++) {
			    String s2 = afile1[k].getName();
			    if (afile1[k].isFile() && s2.startsWith("mod_")
				    && s2.endsWith(".class")) {
				addMod(classloader, s2);
			    } else if (afile1[k].isFile()
				    && s2.startsWith("mod_")
				    && !s2.contains(".MF")) {
				addScript(s2);
			    } else if (afile1[k].isDirectory()) {
				readFromModFolder(afile[k]);
			    }
			}
		    }
		}
	    }
	}
    }

    /**
     * A static method for scripts to use
     * 
     * @return The MojamComponent instance, containing everything.
     * @see MojamComponent
     */
    public static MojamComponent getMojam() {
	return mojam;
    }

    /**
     * Adds a mod that implements IMod to the loaded mods list
     * 
     * @param classloader
     *            The classloader to use
     * @param s
     *            The name and location of the class file
     * @return An instance of the IMod once loaded
     * @see IMod
     */
    static IMod addMod(ClassLoader classloader, String s) {
	try {
	    String s1 = s.split("\\.")[0];
	    Package package1 = (com.mojang.mojam.mod.ModSystem.class)
		    .getPackage();
	    if (package1 != null) {
		s1 = (new StringBuilder(String.valueOf(package1.getName())))
			.append(".")
			.append(s1.substring(s.lastIndexOf('/') + 1))
			.toString();
	    }
	    Class class1 = classloader.loadClass(s1);
	    if (!(IMod.class).isAssignableFrom(class1)) {
		return null;
	    }
	    IMod mod = (IMod) class1.newInstance();
	    if (mod != null) {
		modList.add(mod);
		print("Java Mod Initialized: " + mod.getClass().getSimpleName());
		return mod;
	    }
	} catch (Exception throwable) {
	    throwable.printStackTrace();
	    System.out
		    .println((new StringBuilder("Failed to load mod from \""))
			    .append(s).append("\"").toString());
	}
	return null;
    }

    /**
     * Loads mods from the folder given
     * 
     * @param file
     *            The location of the folder
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static void readFromClassPath(File file)
	    throws FileNotFoundException, IOException {
	ClassLoader classloader = (ModSystem.class).getClassLoader();
	if (file.isFile()
		&& (file.getName().endsWith(".jar") || file.getName().endsWith(
			".zip"))) {
	    print("Reading from classpath " + file.getAbsolutePath());
	    FileInputStream fileinputstream = new FileInputStream(file);
	    ZipInputStream zipinputstream = new ZipInputStream(fileinputstream);
	    Object obj = null;
	    do {
		ZipEntry zipentry = zipinputstream.getNextEntry();
		if (zipentry == null) {
		    break;
		}
		String s1 = zipentry.getName();
		if (!zipentry.isDirectory() && s1.contains("mod_")
			&& s1.endsWith(".class")) {
		    addMod(classloader, s1);
		} else if (!zipentry.isDirectory() && s1.contains("mod_")
			&& !s1.toLowerCase().endsWith(".mf")) {
		    addScript(zipentry);
		}
	    } while (true);
	    fileinputstream.close();
	} else if (file.isDirectory()) {
	    try {
		readFromModFolder(file);
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	    Package package1 = (ModSystem.class).getPackage();
	    if (package1 != null) {
		String s = package1.getName().replace('.', File.separatorChar);
		file = new File(file, s);
	    }
	    File afile[] = file.listFiles();
	    if (afile != null) {
		for (int i = 0; i < afile.length; i++) {
		    String s2 = afile[i].getName();
		    if (afile[i].isFile() && s2.startsWith("mod_")
			    && s2.endsWith(".class")) {
			addMod(classloader, s2);
		    } else if (afile[i].isFile() && s2.contains("mod_")
			    && !s2.toLowerCase().endsWith(".mf")) {
			addScript(afile[i].getAbsolutePath());
		    }
		}
	    }
	}
    }

    /**
     * Loads a script by its filetype
     * 
     * @param s
     *            The location of the script file
     * @return An instance of the script
     */
    static ScriptEngine addScript(String s) {
	if (s.startsWith("http")) {
	    s = new File(mojam.getMojamDir(), "/mods"
		    + s.substring(s.lastIndexOf('/'))).getAbsolutePath();
	}
	if (new File(s).isDirectory()) {
	    return null;
	}
	ScriptEngine e = lang.getEngineByExtension(s.substring(s
		.lastIndexOf('.') + 1));
	try {
	    FileReader fr = new FileReader(s);
	    InputStreamReader library = new InputStreamReader(
		    ModSystem.class.getResourceAsStream("/script/lib/lib."
			    + s.substring(s.lastIndexOf('.') + 1)));
	    e.eval(library);
	    e.eval(fr);
	    e.put("Mojam", mojam);
	    scriptList.add(e);
	    print(e.getFactory().getExtensions().get(0).toUpperCase()
		    + " Script initialised: " + s);
	} catch (FileNotFoundException e1) {
	} catch (NullPointerException e1) {
	    err("Could not initialise mod " + s);
	} catch (Exception e1) {
	}
	return e;
    }

    private static ScriptEngine addScript(ZipEntry entry) {
	String s = entry.getName();
	ScriptEngine engine = lang.getEngineByExtension(s.substring(s
		.lastIndexOf('.') + 1));
	if (engine == null || entry.getName().contains("MANIFEST"))
	    return null;
	try {
	    final int BUFFER = 16284;
	    BufferedOutputStream dest = null;
	    BufferedInputStream is = null;
	    ZipFile zipfile = new ZipFile(modDir.getAbsolutePath());
	    Enumeration e = zipfile.entries();
	    while (e.hasMoreElements()) {
		entry = (ZipEntry) e.nextElement();
		print("Extracting: " + entry);
		is = new BufferedInputStream(zipfile.getInputStream(entry));
		int count;
		byte data[] = new byte[BUFFER];
		FileOutputStream fos = new FileOutputStream(entry.getName());
		dest = new BufferedOutputStream(fos, BUFFER);
		while ((count = is.read(data, 0, BUFFER)) != -1) {
		    dest.write(data, 0, count);
		}
		dest.flush();
		dest.close();
		is.close();
		FileReader fr = new FileReader(entry.getName());

		engine.eval(fr);
		engine.put("ModSystem", new ModSystem());
		scriptList.add(engine);
		print(engine.getFactory().getExtensions().get(0).toUpperCase()
			+ " Script initialised: " + s);
	    }
	} catch (FileNotFoundException e1) {
	    if (!isJar)
		e1.printStackTrace();
	} catch (NullPointerException e1) {
	    err("Could not initialise mod " + s);
	} catch (ScriptException e1) {
	    err("Bad Script file: " + entry.getName());
	    e1.printStackTrace();
	} catch (IOException e1) {
	    e1.printStackTrace();
	}
	return engine;
    }

    /**
     * Gets an instance of an entity from the mods and scripts
     * 
     * @param i
     *            The entity's id
     * @param x
     *            The x coordinate to spawn at
     * @param y
     *            The y coordinate to spawn at
     * @return A new instance of entity with id of i
     * @see Entity
     * @see Mod
     */
    public static Entity getEntityById(int i, double x, double y) {
	for (IMod m : modList) {
	    Entity e = m.getEntityInstanceById(i, x, y);
	    if (e != null) {
		return e;
	    }
	}
	for (ScriptEngine sc : scriptList) {
	    try {
		Entity e = (Entity) ((Invocable) sc).invokeFunction(
			"getEntityInstanceById", new Object[] { i, x, y });
		if (e != null) {
		    return e;
		}
	    } catch (NoSuchMethodException e) {
	    } catch (ScriptException e) {
	    }
	}
	return null;
    }

    /**
     * Used by the spawners
     * 
     * @return The total number of entity types
     */
    public static int numEntitiesLoaded() {
	return spawnList.size();
    }

    /**
     * Registers an instance of the entity into a List and then returns its id,
     * so that it can later be used for spawning custom entities.
     * 
     * @param entityClass
     *            The class of the entity to load
     * @return The id of the registered entity
     * @see Entity
     */
    public static int addEntity(Class entityClass) {
	spawnList.put(spawnList.size(), entityClass);
	int i = spawnList.size() - 1;
	print("Registered " + spawnList.get(i).getSimpleName() + " with id "
		+ i);
	return i;
    }

    public void registerWeapon(Class<? extends IWeapon> weapon, String name,
	    String text) {
	mojam.console.give.registerWeapon(weapon, name, text);
    }

    /**
     * A hook to get console from scripts
     * 
     * @return The console
     */
    public static Console getConsole() {
	return mojam.console;
    }

    /**
     * Checks if the console command has been added successfully. Used as a
     * check rather than a straight function, since Command adds to the list in
     * its constructor. This checks that has happened.
     * 
     * @param c
     *            Command to check
     * @return Whether the command has been added
     */
    public static boolean addConsoleCommand(Console.Command c) {
	return Console.Command.commands.contains(c);
    }

    /**
     * Registers a new key <br>
     * {@code Key mykey = ModSystem.addKey("aKey","a");}
     * 
     * @param name
     *            The name of the key
     * @param code
     *            The keyboard code of the key to be registered
     * @return The registered key
     */
    public static Key addkey(String name, String code) {
	return addKey(name, keycode(code));
    }

    /**
     * Registers a new key <br>
     * {@code Key mykey = ModSystem.addKey("aKey",32);}
     * 
     * @param name
     *            The name of the key
     * @param code
     *            The keyboard code of the key to be registered
     * @return The registered key
     */
    public static Key addKey(String name, int code) {
	return addKey(keys.new Key(name), code);
    }

    /**
     * Registers a given key
     * 
     * @param key
     *            The key being registered
     * @param code
     *            The keyboard code of the key being registered
     * @return The key once registered
     */
    public static Key addKey(Key key, int code) {
	inputHandler = (InputHandler) reflectField(mojam, "inputHandler");
	for (Keys k : mojam.synchedKeys) {
	    k.getAll().add(key);
	}
	mojam.keys.getAll().add(key);
	reflectMethod(InputHandler.class, inputHandler, "initKey",
		new Object[] { key, code });
	print("Added key: " + key.name + " with keycode: " + code);
	return key;
    }

    /**
     * Gets the integer keycode for the name s
     * 
     * @param s
     *            The name of the key
     * @return The intcode of the key
     */
    public static int keycode(String s) {
	Field f = (Field) reflectField(java.awt.event.KeyEvent.class,
		"VK_" + s.toUpperCase());
	try {
	    return (Integer) f.get(null);
	} catch (IllegalArgumentException e) {
	    e.printStackTrace();
	} catch (IllegalAccessException e) {
	    e.printStackTrace();
	}
	return -1;
    }

    /**
     * Used to add an animated bitmap
     * 
     * @param src
     *            The location of the bitmap file
     * @return A bitmap used for rendering. The Bitmap[] holds Bitmap[]s for
     *         each animation, e.g. <br>
     *         {@code
     * Bitmap[][] myBitmap = addAnimation(srcString);
     * return myBitmap[animationId][frameId];
	  * }
     */
    public static AbstractBitmap[][] addAnimation(String src) {
	return mojam.screen.cut(src, 32, 32);
    }

    /**
     * Used to add a still-image bitmap
     * 
     * @param src
     *            The location of the bitmap file
     * @return A bitmap used for rendering
     */
    public static AbstractBitmap addArt(String src) {
	return load(src);
    }

    /**
     * Invokes a reflected method
     * 
     * @param o
     *            The instance that contains the method to be reflected
     * @param s
     *            The name of the method
     * @param params
     *            The parameters of the method
     * @return The value that the method being reflected returns
     */
    public static Object reflectMethod(Object o, String s, Object params[]) {
	// Go and find the private method...
	final Method methods[] = o.getClass().getDeclaredMethods();
	for (int i = 0; i < methods.length; ++i) {
	    if (s.equals(methods[i].getName())) {
		try {
		    methods[i].setAccessible(true);
		    return methods[i].invoke(o, params);
		} catch (IllegalAccessException ex) {

		} catch (InvocationTargetException ite) {

		}
	    }
	}
	return null;
    }

    /**
     * Invokes a method in a declared class
     * 
     * @param c
     *            The class the instance is of
     * @param o
     *            The instance of the class
     * @param s
     *            The name of the method
     * @param params
     *            The parameters of the method
     * @return The returned value of the method
     */
    public static Object reflectMethod(Class c, Object o, String s,
	    Object params[]) {
	// Go and find the private method...
	final Method methods[] = c.getDeclaredMethods();
	for (int i = 0; i < methods.length; ++i) {
	    if (s.equals(methods[i].getName())) {
		try {
		    methods[i].setAccessible(true);
		    return methods[i].invoke(o, params);
		} catch (IllegalAccessException ex) {

		} catch (InvocationTargetException ite) {

		}
	    }
	}
	return null;
    }

    /**
     * Sets a reflected field that would otherwise be hidden
     * 
     * @param o
     *            The instance that holds the field to be reflected
     * @param s
     *            The name of the field to be set
     * @param value
     *            The new value for the field
     * @return The field's new value. It is successful if it returns 'value'.
     */
    public static Object reflectField(Object o, String s, Object value) {
	// Go and find the private field...
	final Field fields[] = o.getClass().getDeclaredFields();
	for (int i = 0; i < fields.length; ++i) {
	    if (s.equals(fields[i].getName())) {
		try {
		    fields[i].setAccessible(true);
		    fields[i].set(o, value);
		    return fields[i].get(o);
		} catch (IllegalAccessException ex) {

		}
	    }
	}
	return null;
    }

    /**
     * Gets a reflected value that would otherwise be hidden
     * 
     * @param o
     *            The instance that holds the field to be reflected
     * @param s
     *            The name of the field to be reflected
     * @return the reflected value of the field
     */
    public static Object reflectField(Object o, String s) {
	// Go and find the private fields...
	final Field fields[] = o.getClass().getDeclaredFields();
	for (int i = 0; i < fields.length; ++i) {
	    if (s.equals(fields[i].getName())) {
		try {
		    fields[i].setAccessible(true);
		    return fields[i].get(o);
		} catch (IllegalAccessException ex) {

		}
	    }
	}
	return null;
    }

    /**
     * Reflects a static field from a {@link Class}
     * 
     * @param c
     *            The class to reflect the variable from
     * @param s
     *            The name of the variable
     * @return The field reflected
     */
    public static Object reflectField(Class c, String s) {
	// Go and find the private fields...
	final Field fields[] = c.getDeclaredFields();
	for (int i = 0; i < fields.length; ++i) {
	    if (s.equals(fields[i].getName())) {
		fields[i].setAccessible(true);
		return fields[i];
	    }
	}
	return null;
    }

    /**
     * Gets a Bitmap
     * 
     * @param string
     *            The bitmap to load
     * @return The bitmap
     */
    private static AbstractBitmap load(String string) {
	try {
	    BufferedImage bi = ImageIO.read(MojamComponent.class
		    .getResource(string));

	    int w = bi.getWidth();
	    int h = bi.getHeight();

	    AbstractBitmap result = mojam.screen.createBitmap(w, h);
	    bi.getRGB(0, 0, w, h, result.getPixels(), 0, w);

	    return result;
	} catch (IOException e) {
	    e.printStackTrace();
	}

	return null;
    }

    public static void afterRender() {
	for (IMod m : modList) {
	    m.OnRender();
	}
	invoke("OnRender");
    }

    public static void startRender() {
	for (IMod m : modList) {
	    m.OnStartRender();
	}
	invoke("OnStartRender");
    }

    public static void afterTick() {
	for (IMod m : modList) {
	    m.AfterTick();
	}
	invoke("AfterTick");
    }

    public static void runOnce() {
	for (IMod m : modList) {
	    m.RunOnce();
	}
	invoke("RunOnce");
    }

    public static void createLevel(Level l) {
	for (IMod m : modList) {
	    m.CreateLevel(l);
	}
	level = l;
	invoke("CreateLevel");
    }

    public static void onStop() {
	for (IMod m : modList) {
	    m.OnClose();
	}
	invoke("OnStop");
    }

    public static void onWin(int i) {
	for (IMod m : modList) {
	    m.OnVictory(i);
	}
	invoke("OnVictory", i);
    }

    public static void levelTick(Level level) {
	for (IMod m : modList) {
	    m.OnLevelTick(level);
	}
	invoke("OnLevelTick", level);
    }

    public static void updateTick() {
	for (IMod m : modList) {
	    m.OnTick();
	}
	invoke("OnTick");
    }

    public static boolean runConsole(String command, String params) {
	int i = 0;
	for (IMod m : modList) {
	    i += m.OnConsole(command, params);
	}
	ScriptEngine engine = lang.getEngineByExtension(command.substring(1));
	if (engine != null) {
	    try {
		engine.eval(params.substring(params.indexOf(" ")));
		i++;
	    } catch (ScriptException e) {
		e.printStackTrace();
	    }
	}
	invoke("OnConsole", command.substring(1), params);
	return i != 0;
    }

    public static void sendPacket(Object packet) {
	for (IMod m : modList) {
	    m.OnSendPacket(packet);
	}
	invoke("OnSendPacket", packet);
    }

    public static void receivePacket(Object packet) {
	for (IMod m : modList) {
	    m.OnReceivePacket(packet);
	}
	invoke("OnReceivePacket", packet);
    }

    public static void handlePacket(Object packet) {
	for (IMod m : modList) {
	    m.HandlePacket(packet);
	}
	invoke("HandlePacket", packet);
    }

    /**
     * Invokes a function or method in a script
     * 
     * @param s
     *            The Method Name
     * @param args
     *            The args to pass to the script
     */
    private static void invoke(String s, Object... args) {
	for (ScriptEngine sc : scriptList) {
	    String sarg = "";
	    if (args.length > 0) {
		for (Object o : args) {
		    sc.put(o.getClass().getSimpleName(), o);
		    sarg += o.getClass().getSimpleName();
		}
	    }
	    try {
		Invocable i = (Invocable) sc;
		i.invokeFunction(s, args);
	    } catch (ClassCastException e) {
	    } catch (ScriptException e) {
		err("Bad method in mod" + " at method " + s + " line: "
			+ e.getLineNumber() + " column: " + e.getColumnNumber());
		e.printStackTrace();
	    } catch (Exception e) {
	    }
	}
    }

    /**
     * Gets the current system time
     * 
     * @return current system time in milliseconds
     */
    public static long currentTimeMillis() {
	return System.currentTimeMillis();
    }

    /**
     * Gets the current system time
     * 
     * @return current system time in nanoseconds
     */
    public static long nanoTime() {
	return System.nanoTime();
    }

    /**
     * Gets an instance of Font to avoid static method semantics in scripts
     * 
     * @return an instance of the class {@link Font} for non-statically calling
     *         static code
     */
    public static Font getFont() {
	return Font.defaultFont();
    }

    /**
     * Since JRuby doesn't like static methods for $mod
     */
    @Deprecated
    public Font font() {
	return Font.defaultFont();
    }

    /**
     * Gets the instance of Screen to avoid static method semantics in scripts
     * 
     * @return the instance of Screen
     */
    public static AbstractScreen getScreen() {
	return mojam.screen;
    }

    /**
     * Since JRuby doesn't like static methods for $mod
     * 
     * @return the instance of Screen
     */
    @Deprecated
    public AbstractScreen screen() {
	return mojam.screen;
    }

    /**
     * Shortcut to the Font.defaultFont.draw() issues with scripts and
     * namespaces
     */
    public static void draw(String s, int i, int j) {
	Font.defaultFont().draw(mojam.screen, s, i, j);
    }

    /**
     * Gets an instance of Options to avoid static method semantics in scripts
     * 
     * @return an instance of the class {@link Options} for non statically
     *         calling static code
     */
    public static Options getOptions() {
	return new Options();
    }

    /**
     * Returns a new instance of an Empty Entity for JS or others to manipulate;
     * i.e: <br>
     * {@code newEntity(x,y).getSpeed = new function return 1.5; }
     * 
     * @param x
     *            x position to spawn
     * @param y
     *            y position to spawn
     * @return new instance to modify
     * @see Entity
     * @see EmptyEntity
     */
    public static Entity newEntity(double x, double y) {
	return new EmptyEntity(x, y);
    }

    /**
     * Sets the gamemode
     * 
     * @param gamemode
     *            Gamemode to play
     */
    public static void setGamemode(GameMode gamemode) {
	TitleMenu.defaultGameMode = gamemode;
    }

    /**
     * Used for impromptu class casting in dynamic languages
     * 
     * @param d
     *            var to pass
     * @return d as long
     */
    public static long asLong(double d) {
	return (long) d;
    }

    @Deprecated
    private static void printAllLangFactories() {
	for (ScriptEngineFactory sef : lang.getEngineFactories()) {
	    for (String s : sef.getExtensions()) {
		debug(s);
	    }
	}
    }

    public static void handleNetworkCommand(int playerId, Object packet) {
	if (packet instanceof Network.ChangeKeyMessage) {
	    ChangeKeyMessage ckc = (ChangeKeyMessage) packet;
	    Key key = mojam.synchedKeys[playerId].getAll().get(ckc.key);
	    if (key.isDown) {
		for (IMod m : modList) {
		    m.IfKeyDown(key);
		}
	    } else {
		for (IMod m : modList) {
		    m.IfKeyUp(key);
		}
	    }
	}
    }

    /**
     * Downloads a file
     * 
     * @param path
     *            The URL of the file to download
     * @param dest
     *            The destination to put the file
     * @param info
     *            Additional optional data
     * 
     * @return The File once downloaded
     * @throws IOException
     */
    public static File downloadFile(String path, String dest, String info)
	    throws IOException {
	File f, f1;
	f = new File(path);
	f1 = new File(dest);
	if (!f1.exists() && !f1.createNewFile())
	    throw new IOException("Could not create file at "
		    + f1.getCanonicalPath());
	URL url = new URL(path);
	ReadableByteChannel rbc = Channels.newChannel(url.openStream());
	FileOutputStream fos = new FileOutputStream(f1);
	fos.getChannel().transferFrom(rbc, 0, 1 << 24);
	print("Downloaded " + info + " to: " + dest);
	return new File(dest);
    }
    
    public static void print(Object o) {
	System.out.println(o);
    }

    public static void err(Object o) {
	System.err.println(o);
    }

    public static void debug(Object... o) {
	String result = "";
	for (Object o1 : o) {
	    result += "-[" + o1.toString() + "]-";
	}
	System.out.println(result);
    }

}
