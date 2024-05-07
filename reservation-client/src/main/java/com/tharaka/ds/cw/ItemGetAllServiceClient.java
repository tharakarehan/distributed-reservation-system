package com.tharaka.ds.cw;

import common.tharaka.ds.cw.communication.grpc.generated.*;
import io.grpc.ManagedChannel;

import java.util.Scanner;

public class ItemGetAllServiceClient {
    private ManagedChannel channel = null;
    ItemSearchServiceGrpc.ItemSearchServiceBlockingStub clientStub = null;
    String host = null;
    int port = -1;

    public ItemGetAllServiceClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void initializeConnection (ManagedChannel channel) {
        System.out.println("Initializing Connecting to server at " + host + ":" +
                port);
        this.channel = channel;
        clientStub = ItemSearchServiceGrpc.newBlockingStub(this.channel);
    }
    public void closeConnection() {
        channel.shutdown();
    }

    public void processUserRequests(Scanner userInput) throws InterruptedException {
        System.out.println("\nEnter your username :");
        String userName = userInput.nextLine().trim();
        System.out.println("Requesting server to get items by " + userName);
        UserRequest request = UserRequest.newBuilder().setUserName(userName).build();
        ItemGetAllResponse response = clientStub.getAllItems(request);

        // Print column names
        System.out.printf("%n+-------------------------------------------------------------------------------------------------------------------------------------+%n");
        System.out.printf("%n| %-50s| %-12s| %-15s| %-10s| %-20s|%-16s|%n", "Item Id",
                "Item Name", "Type", "Price", "Available Quantity", "Seller Name");
        System.out.printf("%n+-------------------------------------------------------------------------------------------------------------------------------------+%n");

        for (ItemDTO item : response.getItemsList()) {
            // Print item details in a tabular format
            System.out.printf("%n| %-50s| %-12s| %-15s| %-10s| %-20s|%-16s|%n", item.getItemId(),
                    item.getItemName(), item.getType(), item.getPrice(), item.getAvailableQuantity(), item.getSellerName());
            // Print horizontal line
            System.out.printf("%n+-------------------------------------------------------------------------------------------------------------------------------------+%n");
        }
        Thread.sleep(1000);
    }
}
