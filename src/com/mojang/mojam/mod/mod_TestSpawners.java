package com.mojang.mojam.mod;

import java.awt.event.KeyEvent;

import com.mojang.mojam.Keys.Key;
import com.mojang.mojam.entity.Entity;
import com.mojang.mojam.entity.mob.Mob;
import com.mojang.mojam.entity.mob.TestEntity;

public class mod_TestSpawners extends Mod
{

	int id;
	int turrentId;
	long invulnTimer = 0;
	long frame = 0;
	long lastFrame = 0;
	int fps = 0;
	int placeBomb = KeyEvent.VK_B;

	public mod_TestSpawners()
	{
		id = ModSystem.addEntity(TestEntity.class);
	}

	@Override
	public Entity getEntityInstanceById(int i, double x, double y)
	{
		Mob te = null;
		if(i == id) te = new TestEntity(x, y);
		return te;
	}
	
	@Override
	public void OnKeyPressed(KeyEvent k)
	{
		System.out.println("Pressed: "+k.getKeyChar());
		if(k.getKeyCode()==(placeBomb))System.out.println("Bomb Pressed!");
	}	

	@Override
	public String getVersion()
	{
		return "Test";
	}

}
