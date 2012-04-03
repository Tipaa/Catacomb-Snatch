package com.mojang.mojam.mod;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import com.mojang.mojam.MojamComponent;


public class KeyListenerProxy implements KeyListener{

    @Override
    public void keyPressed(KeyEvent event) {
	ModSystem.onKeyPressed(event);
    }

    @Override
    public void keyReleased(KeyEvent event) {
	ModSystem.onKeyReleased(event);
    }

    @Override
    public void keyTyped(KeyEvent event) {
	
    }

}
