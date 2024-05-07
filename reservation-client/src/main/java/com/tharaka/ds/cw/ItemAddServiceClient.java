package com.tharaka.ds.cw;

import common.tharaka.ds.cw.communication.grpc.generated.*;
import io.grpc.ManagedChannel;

import java.util.Scanner;
import java.util.UUID;

public class ItemAddServiceClient {
    private ManagedChannel channel = null;
    ItemAddServiceGrpc.ItemAddServiceBlockingStub clientStub = null;
    String host = null;
    int port = -1;

    public ItemAddServiceClient (String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void initializeConnection (ManagedChannel channel) {
        System.out.println("Initializing Connecting to server at " + host + ":" +
                port);
        this.channel = channel;
        clientStub = ItemAddServiceGrpc.newBlockingStub(this.channel);
    }
    public void closeConnection() {
        channel.shutdown();
    }

    public void processUserRequests(Scanner userInput) throws InterruptedException {
        System.out.println("\nEnter Item Name, Type, Price, Available Quantity, Seller Name to add an item :");
        String input[] = userInput.nextLine().trim().split(",");
        String itemName = input[0];
        String type = input[1];
        double price = Double.parseDouble(input[2]);
        int availableQuantity = Integer.parseInt(input[3]);
        String sellerName = input[4];
        System.out.println("Requesting server to add item " + itemName);
        ItemAddRequest request = ItemAddRequest
                .newBuilder()
                .setItemId(itemName + "_" + UUID.randomUUID())
                .setItemName(itemName)
                .setType(getType(type))
                .setPrice(price)
                .setAvailableQuantity(availableQuantity)
                .setSellerName(sellerName)
                .setIsSentByPrimary(false)
                .build();
        StatusResponse response = clientStub.addItem(request);
        System.out.printf("Process Add Item " + response.getStatus() + ". " + response.getMessage() + "\n");
        Thread.sleep(1000);
    }

    private Type getType(String type) {
        if (type.equalsIgnoreCase("NEW_ARRIVAL")) {
            return Type.NEW_ARRIVAL;
        } else if (type.equalsIgnoreCase("SELL")) {
            return Type.SELL;
        } else {
            return Type.RENT;
        }
    }
}
