package com.tharaka.ds.cw;

import common.tharaka.ds.cw.communication.grpc.generated.ItemDeleteRequest;
import common.tharaka.ds.cw.communication.grpc.generated.ItemDeleteServiceGrpc;
import common.tharaka.ds.cw.communication.grpc.generated.ItemUpdateRequest;
import common.tharaka.ds.cw.communication.grpc.generated.StatusResponse;
import io.grpc.ManagedChannel;

import java.util.Scanner;

public class ItemDeleteServiceClient {
    private ManagedChannel channel = null;
    ItemDeleteServiceGrpc.ItemDeleteServiceBlockingStub clientStub = null;
    String host = null;
    int port = -1;

    public ItemDeleteServiceClient (String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void initializeConnection (ManagedChannel channel) {
        System.out.println("Initializing Connecting to server at " + host + ":" +
                port);
        this.channel = channel;
        clientStub = ItemDeleteServiceGrpc.newBlockingStub(this.channel);
    }
    public void closeConnection() {
        channel.shutdown();
    }

    public void processUserRequests(Scanner userInput) throws InterruptedException {
        System.out.println("\nEnter Item Id, Seller Name to delete an item :");
        String input[] = userInput.nextLine().trim().split(",");
        String itemId = input[0];
        String sellerName = input[1];
        System.out.println("Requesting server to delete item " + itemId);
        ItemDeleteRequest request = ItemDeleteRequest
                .newBuilder()
                .setItemId(itemId)
                .setSellerName(sellerName)
                .setIsSentByPrimary(false)
                .build();
        StatusResponse response = clientStub.deleteItem(request);
        System.out.printf("Process Delete Item " + response.getStatus() + ". " + response.getMessage() + "\n");
        Thread.sleep(1000);
    }
}
