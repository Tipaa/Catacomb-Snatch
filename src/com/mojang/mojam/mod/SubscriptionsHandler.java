package com.mojang.mojam.mod;

import static com.mojang.mojam.mod.ModSystem.downloadFile;
import static com.mojang.mojam.mod.ModSystem.mojam;
import static com.mojang.mojam.mod.ModSystem.print;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubscriptionsHandler {

    static void run() throws IOException {
	readLinksFromFile(new File(mojam.getMojamDir(), "mods.txt"));
    }

    /**
     * Sets up mod subscriptions file
     * 
     * @param f
     *            The location of the subscriptions
     * @throws IOException
     */
    private static void readLinksFromFile(File f) throws IOException {
	
	createSubscriptions(f);
	
	Map mods = readMapFromFile(f);

	List<String> scripts = (List<String>) mods.get('+');
	List<String> dependencies = (List<String>) mods.get('~');
	List<String> offline = (List<String>) mods.get('@');
	List<String> delete = (List<String>) mods.get('$');

	try {
	    downloadFiles(scripts, "script");
	} catch (Exception e) {
	}
	try {
	    downloadFiles(dependencies, "dependency");
	} catch (Exception e) {
	}
	
	if (scripts != null) {
	    for (String s : scripts) {
		if (s != null) {
		    try {
			if (s.endsWith(".class")) {
			    ModSystem.addMod(ModSystem.class.getClassLoader(),
				    s);
			} else {
			    ModSystem.addScript(s);
			}
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		}
	    }
	}
	
	if (offline != null) {
	    for (String s : offline) {
		if (s != null) {
		    try {
			if (s.endsWith(".class")) {
			    ModSystem.addMod(ModSystem.class.getClassLoader(),
				    s);
			} else {
			    ModSystem.addScript(s);
			}
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		}
	    }
	}
	
	if (delete != null) {
	    for (String s : delete) {
		if (s != null) {
		    try {
			if(new File(s).delete()&&!new File(s).exists()){
			    print("Successfully deleted " + s.substring(s.lastIndexOf('/')));
			}
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		}
	    }
	}
    }

    /**
     * Reads a File into a String
     * 
     * @param f
     *            The File to read
     * @return The Contents of the file
     * @throws IOException
     */
    private static Map<Character, List<String>> readMapFromFile(File f)
	    throws IOException {

	Map<Character, List<String>> map = new HashMap<Character, List<String>>();
	Map<Character, String> temp = new HashMap<Character, String>();

	BufferedReader reader = new BufferedReader(new FileReader(f));
	String line = null;
	StringBuilder stringBuilder = new StringBuilder();
	String ls = System.getProperty("line.separator");
	while ((line = reader.readLine()) != null) {
	    if (!line.trim().startsWith("#") && line.trim().length() > 0) {
		stringBuilder.append(line);
		stringBuilder.append(ls);
		String get = temp.get(line.charAt(0));
		if(get == "null"){get = "";}
		get+=ls+line.substring(1);
		temp.put(
			line.charAt(0),
			get);
		// append the line to the correct tempmap key
	    }
	}
	for (char c : temp.keySet()) {
	    map.put(c, Arrays.asList(temp.get(c).split(ls)));
	    // put at key c the array of tempmap's key c when split at line
	    // separator to list
	}
	return map;
    }

    /**
     * Creates the subscriptions file
     * 
     * @param f
     *            The location of the file
     * @throws IOException
     */
    private static void createSubscriptions(File f) throws IOException {
	if (!f.exists()) {
	    while (f.createNewFile()) {
		print("Creating Mod Subscriptions File");
	    }
	    PrintWriter p = new PrintWriter(f);
	    p.println("#####CATACOMB###SNATCH#####");
	    p.println("#");
	    p.println("# Add links to subscribe to mods.");
	    p.println("#");
	    p.println("# Key:");
	    p.println("# +<url> Subscribed Mod");
	    p.println("# -<url> Unsubscribed Mod");
	    p.println("# #<text> Comment");
	    p.println("# @<url> Offline Mod");
	    p.println("# $<url> Remove Mod");
	    p.println("# ~<text> Dependency");
	    p.println("#");
	    p.println("###########################");
	    p.close();
	}
    }

    private static void downloadFiles(List<String> list, String info)
	    throws IOException {
	for (String s : list) {
	    downloadFile(
		    s,
		    mojam.getMojamDir().getAbsolutePath() + "/mods"
			    + s.substring(s.lastIndexOf('/')), info);
	}
    }

}
