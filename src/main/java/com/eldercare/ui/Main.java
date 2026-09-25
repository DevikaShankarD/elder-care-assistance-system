package com.eldercare.ui;

/**
 * Entry point for the Elder Care Assistance System application.
 * Initializes and launches the interactive {@link ConsoleMenu}.
 */
public class Main {

    /**
     * Application main method.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        ConsoleMenu menu = new ConsoleMenu();
        menu.start();
    }
}
