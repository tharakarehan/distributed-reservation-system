package com.tharaka.ds.cw;

import common.tharaka.ds.cw.communication.grpc.generated.*;
import io.grpc.ManagedChannel;

import java.util.Scanner;

public class ItemUpdateServiceClient {
    private ManagedChannel channel = null;
    ItemUpdateServiceGrpc.ItemUpdateServiceBlockingStub clientStub = null;
    String host = null;
    int port = -1;

    public ItemUpdateServiceClient (String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void initializeConnection (ManagedChannel channel) {
        System.out.println("Initializing Connecting to server at " + host + ":" +
                port);
        this.channel = channel;
        clientStub = ItemUpdateServiceGrpc.newBlockingStub(this.channel);
    }
    public void closeConnection() {
        channel.shutdown();
    }

    public void processUserRequests(Scanner userInput) throws InterruptedException {
        System.out.println("\nEnter Item Id, Seller Name, Price, Available Quantity to update an item :");
        String input[] = userInput.nextLine().trim().split(",");
        String itemId = input[0];
        String sellerName = input[1];
        double price = Double.parseDouble(input[2]);
        int availableQuantity = Integer.parseInt(input[3]);
        System.out.println("Requesting server to update item " + itemId);
        ItemUpdateRequest request = ItemUpdateRequest
                .newBuilder()
                .setItemId(itemId)
                .setSellerName(sellerName)
                .setPrice(price)
                .setAvailableQuantity(availableQuantity)
                .setIsSentByPrimary(false)
                .build();
        StatusResponse response = clientStub.updateItem(request);
        System.out.printf("Process Update Item " + response.getStatus() + ". " + response.getMessage() + "\n");
        Thread.sleep(1000);
    }
}
