package com.ayman.speardash;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public class SpearDashMod implements ClientModInitializer {

    private static KeyBinding toggleKey;
    private static boolean active = false;
    private static boolean previousKeyState = false;

    private int spearSlot = -1;
    private int emptySlot = -1;

    private int step = 0;
    private int waitTicks = 0;

    private static final int DELAY_PER_STEP = 1;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.speardash.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.speardash.controls"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) {
            active = false;
            step = 0;
            waitTicks = 0;
            return;
        }

        boolean isPressed = toggleKey.isPressed();
        if (isPressed && !previousKeyState) {
            active = !active;
            step = 0;
            waitTicks = 0;
            if (active) {
                scanHotbar(client);
            }
        }
        previousKeyState = isPressed;

        if (!active) {
            return;
        }

        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        scanHotbar(client);
        if (spearSlot == -1 || emptySlot == -1) {
            return;
        }

        switch (step) {
            case 0 -> {
                client.player.getInventory().selectedSlot = spearSlot;
                step = 1;
                waitTicks = DELAY_PER_STEP;
            }
            case 1 -> {
                if (client.player.getInventory().selectedSlot == spearSlot) {
                    client.doAttack();
                }
                step = 2;
                waitTicks = DELAY_PER_STEP;
            }
            case 2 -> {
                client.player.getInventory().selectedSlot = emptySlot;
                step = 3;
                waitTicks = DELAY_PER_STEP;
            }
            case 3 -> {
                client.player.getInventory().selectedSlot = spearSlot;
                step = 1;
                waitTicks = DELAY_PER_STEP;
            }
            default -> step = 0;
        }
    }

    private void scanHotbar(MinecraftClient client) {
        spearSlot = -1;
        emptySlot = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            String itemName = stack.getItem().toString().toLowerCase();
            if (itemName.contains("spear") || itemName.contains("halberd") || itemName.contains("lance")) {
                spearSlot = i;
                break;
            }
        }

        if (spearSlot != -1) {
            int left = spearSlot - 1;
            int right = spearSlot + 1;

            if (left >= 0 && client.player.getInventory().getStack(left).isEmpty()) {
                emptySlot = left;
            } else if (right < 9 && client.player.getInventory().getStack(right).isEmpty()) {
                emptySlot = right;
            } else {
                for (int i = 0; i < 9; i++) {
                    if (client.player.getInventory().getStack(i).isEmpty()) {
                        emptySlot = i;
                        break;
                    }
                }
            }
        }
    }
}
plugins {
    id 'fabric-loom' version '1.7.4'
    id 'maven-publish'
}

version = project.mod_version
group = project.maven_group

base {
    archivesName = project.archives_base_name
}

repositories {
    maven {
        name = 'Fabric'
        url = 'https://maven.fabricmc.net/'
    }
}

dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    mappings "net.fabricmc:yarn:${project.yarn_mappings}:v2"
    modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
}

processResources {
    inputs.property "version", project.version
    filesMatching("fabric.mod.json") {
        expand "version": inputs.properties.version
    }
}

tasks.withType(JavaCompile).configureEach {
    it.options.release = 21
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
        }
