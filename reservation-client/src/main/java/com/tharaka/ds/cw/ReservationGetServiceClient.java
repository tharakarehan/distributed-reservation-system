package com.tharaka.ds.cw;

import common.tharaka.ds.cw.communication.grpc.generated.*;
import io.grpc.ManagedChannel;

import java.util.Scanner;

public class ReservationGetServiceClient {
    private ManagedChannel channel = null;
    ReservationGetServiceGrpc.ReservationGetServiceBlockingStub clientStub = null;
    String host = null;
    int port = -1;

    public ReservationGetServiceClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void initializeConnection(ManagedChannel channel) {
        System.out.println("Initializing Connecting to server at " + host + ":" +
                port);
        this.channel = channel;
        clientStub = ReservationGetServiceGrpc.newBlockingStub(this.channel);
    }
    public void closeConnection() {
        channel.shutdown();
    }

    public void processUserRequests(Scanner userInput) throws InterruptedException {
        System.out.println("\nEnter your username :");
        String userName = userInput.nextLine().trim();
        System.out.println("Requesting server to get items by " + userName);
        UserRequest request = UserRequest.newBuilder().setUserName(userName).build();
        ReservationSearchResponse response = clientStub.getReservations(request);

        // Print column names
        System.out.printf("%n+-------------------------------------------------------------------------------------------------------------------------------------------------------------+%n");
        System.out.printf("%n| %-50s| %-50s| %-12s| %-16s| %-12s|%-15s|%-15s|%-16s|%n", "Reservation Id",
                "Item Id", "Item Name", "Buyer Name", "Quantity", "Reservation Date", "Payment Amount", "Seller Name");
        System.out.printf("%n+-------------------------------------------------------------------------------------------------------------------------------------------------------------+%n");

        for (Reservation reservation : response.getReservationsList()) {
            // Print item details in a tabular format
            System.out.printf("%n| %-50s| %-50s| %-12s| %-16s| %-12s|%-15s|%-15s|%-16s|%n", reservation.getReservationId(),
                    reservation.getItemId(), reservation.getItemName(), reservation.getBuyerName(), reservation.getQuantity(),
                    reservation.getReservationDate(), reservation.getPaymentAmount(), reservation.getSellerName());
            // Print horizontal line
            System.out.printf("%n+-------------------------------------------------------------------------------------------------------------------------------------------------------------+%n");
        }
        Thread.sleep(1000);
    }
}
