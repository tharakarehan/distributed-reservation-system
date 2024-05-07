package com.tharaka.ds.cw;

import com.tharaka.ds.cw.utils.PrintUtils;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Scanner;

public class ReservationClient {

    private final UserAddServiceClient userAddClient;
    private final ItemGetAllServiceClient itemGetAllClient;
    private final ItemAddServiceClient itemAddClient;
    private final ItemGetServiceClient itemGetClient;
    private final ReservationServiceClient reservationServiceClient;
    private final ReservationGetServiceClient reservationGetServiceClient;
    private final ItemUpdateServiceClient itemUpdateServiceClient;
    private final ItemDeleteServiceClient itemDeleteServiceClient;

    public ReservationClient(UserAddServiceClient userAddClient, ItemGetAllServiceClient itemGetAllClient,
                             ItemAddServiceClient itemAddClient, ItemGetServiceClient itemGetClient,
                             ReservationServiceClient reservationServiceClient, ReservationGetServiceClient reservationGetServiceClient,
                             ItemUpdateServiceClient itemUpdateServiceClient, ItemDeleteServiceClient itemDeleteServiceClient) {
        this.userAddClient = userAddClient;
        this.itemGetAllClient = itemGetAllClient;
        this.itemAddClient = itemAddClient;
        this.itemGetClient = itemGetClient;
        this.reservationServiceClient = reservationServiceClient;
        this.reservationGetServiceClient = reservationGetServiceClient;
        this.itemUpdateServiceClient = itemUpdateServiceClient;
        this.itemDeleteServiceClient = itemDeleteServiceClient;
    }

    public static void main( String[] args ) {
        String host = args[0];
        int port = Integer.parseInt(args[1].trim());

        if (args.length != 2) {
            System.out.println("Usage ReservationClient <host> <port>");
            System.exit(1);
        }
        PrintUtils.printConsoleAppHeader();
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost",port)
                .usePlaintext()
                .build();
        Scanner userInput = new Scanner(System.in);

        UserAddServiceClient userAddClient = new UserAddServiceClient(host, port);
        ItemAddServiceClient ItemAddClient = new ItemAddServiceClient(host, port);
        ItemGetAllServiceClient ItemGetAllClient = new ItemGetAllServiceClient(host, port);
        ItemGetServiceClient ItemGetClient = new ItemGetServiceClient(host, port);
        ReservationServiceClient reservationServiceClient = new ReservationServiceClient(host, port);
        ReservationGetServiceClient reservationGetServiceClient = new ReservationGetServiceClient(host, port);
        ItemUpdateServiceClient itemUpdateServiceClient = new ItemUpdateServiceClient(host, port);
        ItemDeleteServiceClient itemDeleteServiceClient = new ItemDeleteServiceClient(host, port);
        ReservationClient reservationClient = new ReservationClient(userAddClient, ItemGetAllClient, ItemAddClient, ItemGetClient,
                reservationServiceClient, reservationGetServiceClient, itemUpdateServiceClient, itemDeleteServiceClient);
        reservationClient.initializeConnection(channel);
        reservationClient.startProcessUserRequests(userInput);
        reservationClient.closeConnection();
    }

    private void startProcessUserRequests(Scanner userInput) {
        while (true) {
            PrintUtils.displayMenu();
            String choiceStr = userInput.nextLine();
            try {
                int choice = Integer.parseInt(choiceStr);
                switch (choice) {
                    case 1:
                        userAddClient.processUserRequests(userInput);
                        break;
                    case 2:
                        itemAddClient.processUserRequests(userInput);
                        break;
                    case 3:
                        itemGetAllClient.processUserRequests(userInput);
                        break;
                    case 4:
                        itemGetClient.processUserRequests(userInput);
                        break;
                    case 5:
                        itemUpdateServiceClient.processUserRequests(userInput);
                        break;
                    case 6:
                        itemDeleteServiceClient.processUserRequests(userInput);
                        break;
                    case 7:
                        reservationServiceClient.processUserRequests(userInput);
                        break;
                    case 8:
                        reservationGetServiceClient.processUserRequests(userInput);
                        break;
                    case 9:
                        PrintUtils.printConsoleAppFooter();
                        System.exit(0);
                    default:
                        System.out.println("Invalid choice");
                }
            } catch (Exception e) {
                System.out.println("ERROR: " + e);
            }
        }
    }

    private void initializeConnection(ManagedChannel channel) {
        userAddClient.initializeConnection(channel);
        itemAddClient.initializeConnection(channel);
        itemGetAllClient.initializeConnection(channel);
        itemGetClient.initializeConnection(channel);
        reservationServiceClient.initializeConnection(channel);
        reservationGetServiceClient.initializeConnection(channel);
        itemUpdateServiceClient.initializeConnection(channel);
        itemDeleteServiceClient.initializeConnection(channel);
    }

    private void closeConnection() {
        userAddClient.closeConnection();
        itemAddClient.closeConnection();
        itemGetAllClient.closeConnection();
        itemGetClient.closeConnection();
        reservationServiceClient.closeConnection();
        reservationGetServiceClient.closeConnection();
        itemUpdateServiceClient.closeConnection();
        itemDeleteServiceClient.closeConnection();
    }
}
