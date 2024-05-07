package com.tharaka.ds.cw.utils;

public class PrintUtils {

    private PrintUtils() {
    }

    public static void displayMenu(){
        printReservationClientMenuHeader();
        System.out.println("1. Create Users");
        System.out.println("2. Add Items");
        System.out.println("3. Show Items");
        System.out.println("4. List Items by Seller");
        System.out.println("5. Update Item");
        System.out.println("6. Delete Item");
        System.out.println("7. Make Reservation");
        System.out.println("8. Show Reservations");
        System.out.println("9. Exit");
        printConsoleAppMenuFooter();
        System.out.print("Enter your choice: ");
    }

    private static void printReservationClientMenuHeader() {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                   Reservation Client Menu                    ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
    }

    private static void printConsoleAppMenuFooter() {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
    }

    public static void printConsoleAppHeader() {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║               Reservation Client System Start                ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
    }

    public static void printConsoleAppFooter() {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                Reservation Client System End                 ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
    }
}
