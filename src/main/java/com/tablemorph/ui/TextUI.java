package com.tablemorph.ui;

import java.util.Scanner;

/**
 * Utility class for text-based user interface operations.
 */
public class TextUI {
    private static final Scanner scanner = new Scanner(System.in);
    
    /**
     * Private constructor to prevent instantiation.
     */
    private TextUI() {}
    
    /**
     * Displays the application logo.
     */
    public static void displayLogo() {
        System.out.println("=======================================================");
        System.out.println(" _____     _     _      __  __                _       ");
        System.out.println("|_   _|_ _| |__ | | ___|  \\/  | ___  _ __ _ __| |_     ");
        System.out.println("  | |/ _` | '_ \\| |/ _ \\ |\\/| |/ _ \\| '__| '_ \\ __|");
        System.out.println("  | | (_| | |_) | |  __/ |  | | (_) | |  | |_) | |_    ");
        System.out.println("  |_|\\__,_|_.__/|_|\\___|_|  |_|\\___/|_|  | .__/ \\__|");
        System.out.println("                                         |_|         ");
        System.out.println("=======================================================");
        System.out.println("  Wavetable Generator for Vital by Proxeeus (v1.1)    ");
        System.out.println("=======================================================");
    }
    
    /**
     * Gets a line of input from the user.
     * 
     * @return The user's input
     */
    public static String readLine() {
        return scanner.nextLine().trim();
    }
    
    /**
     * Gets an integer input from the user within a range.
     * 
     * @param prompt The prompt to display
     * @param min The minimum allowed value
     * @param max The maximum allowed value
     * @param defaultValue The default value if input is invalid
     * @return The user's choice
     */
    public static int readInt(String prompt, int min, int max, int defaultValue) {
        System.out.print(prompt);
        
        try {
            int value = Integer.parseInt(readLine());
            if (value >= min && value <= max) {
                return value;
            } else {
                System.out.println("[ERROR] Value must be between " + min + " and " + max + ". Using default: " + defaultValue);
                return defaultValue;
            }
        } catch (NumberFormatException e) {
            System.out.println("[ERROR] Invalid input. Using default: " + defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Gets a double input from the user within a range.
     * 
     * @param prompt The prompt to display
     * @param min The minimum allowed value
     * @param max The maximum allowed value
     * @param defaultValue The default value if input is invalid
     * @return The user's choice
     */
    public static double readDouble(String prompt, double min, double max, double defaultValue) {
        System.out.print(prompt);
        
        try {
            double value = Double.parseDouble(readLine());
            if (value >= min && value <= max) {
                return value;
            } else {
                System.out.println("[ERROR] Value must be between " + min + " and " + max + ". Using default: " + defaultValue);
                return defaultValue;
            }
        } catch (NumberFormatException e) {
            System.out.println("[ERROR] Invalid input. Using default: " + defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Gets a boolean input from the user (y/n).
     * 
     * @param prompt The prompt to display
     * @param defaultValue The default value if input is invalid
     * @return The user's choice
     */
    public static boolean readBoolean(String prompt, boolean defaultValue) {
        System.out.print(prompt + " (y/n): ");
        
        String input = readLine().toLowerCase();
        if (input.startsWith("y")) {
            return true;
        } else if (input.startsWith("n")) {
            return false;
        } else {
            System.out.println("[ERROR] Invalid input. Using default: " + (defaultValue ? "yes" : "no"));
            return defaultValue;
        }
    }
    
    /**
     * Displays a message and waits for the user to press Enter.
     * 
     * @param message The message to display (null for no message)
     */
    public static void waitForEnter(String message) {
        if (message != null) {
            System.out.println(message);
        }
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }
    
    /**
     * Displays a section header with a title.
     * 
     * @param title The title to display
     */
    public static void displayHeader(String title) {
        System.out.println("\n╔════════════════════════════════════╗");
        System.out.println("║  " + title + getSpaces(30 - title.length()) + "║");
        System.out.println("╚════════════════════════════════════╝");
    }
    
    /**
     * Displays a box with a title and content.
     * 
     * @param title The title of the box
     * @param content The content to display (array of strings)
     */
    public static void displayBox(String title, String[] content) {
        System.out.println("\n╔════════════════════════════════════╗");
        System.out.println("║  " + title + getSpaces(30 - title.length()) + "║");
        System.out.println("╠════════════════════════════════════╣");
        
        for (String line : content) {
            // Ensure line fits in the box, truncate if needed
            String displayLine = line;
            if (displayLine.length() > 32) {
                displayLine = displayLine.substring(0, 29) + "...";
            }
            System.out.println("║  " + displayLine + getSpaces(30 - displayLine.length()) + "║");
        }
        
        System.out.println("╚════════════════════════════════════╝");
    }
    
    /**
     * Generates a string of spaces of a specified length.
     * 
     * @param count The number of spaces
     * @return A string containing the spaces
     */
    public static String getSpaces(int count) {
        if (count <= 0) return "";
        
        StringBuilder spaces = new StringBuilder();
        for (int i = 0; i < count; i++) {
            spaces.append(" ");
        }
        return spaces.toString();
    }
} 