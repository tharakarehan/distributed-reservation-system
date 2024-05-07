package com.tharaka.ds.cw;

import common.tharaka.ds.cw.communication.grpc.generated.*;
import io.grpc.ManagedChannel;

import java.util.Scanner;
import java.util.UUID;

public class ReservationServiceClient {
    private ManagedChannel channel = null;
    ReserveServiceGrpc.ReserveServiceBlockingStub clientStub = null;
    String host = null;
    int port = -1;

    public ReservationServiceClient (String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void initializeConnection (ManagedChannel channel) {
        System.out.println("Initializing Connecting to server at " + host + ":" +
                port);
        this.channel = channel;
        clientStub = ReserveServiceGrpc.newBlockingStub(this.channel);
    }
    public void closeConnection() {
        channel.shutdown();
    }

    public void processUserRequests(Scanner userInput) throws InterruptedException {
        System.out.println("\nEnter Item Id, Buyer Name, Quantity, Date to create a reservation :");
        String input[] = userInput.nextLine().trim().split(",");
        String itemId = input[0];
        String buyerName = input[1];
        int quantity = Integer.parseInt(input[2]);
        String date = input[3];
        System.out.println("Requesting server to create a reservation on " + itemId + " by " + buyerName);
        ReserveRequest request = ReserveRequest
                .newBuilder()
                .setItemId(itemId)
                .setReservationId(UUID.randomUUID().toString())
                .setBuyerName(buyerName)
                .setQuantity(quantity)
                .setReservationDate(date)
                .setIsSentByPrimary(false)
                .build();
        StatusResponse response = clientStub.reserveItem(request);
        System.out.printf("Process Create Reservation " + response.getStatus() + ". " + response.getMessage() + "\n");
        Thread.sleep(1000);
    }
}
