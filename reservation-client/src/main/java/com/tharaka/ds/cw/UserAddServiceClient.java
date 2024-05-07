package com.tharaka.ds.cw;

import common.tharaka.ds.cw.communication.grpc.generated.Role;
import common.tharaka.ds.cw.communication.grpc.generated.StatusResponse;
import common.tharaka.ds.cw.communication.grpc.generated.UserAddRequest;
import common.tharaka.ds.cw.communication.grpc.generated.UserAddServiceGrpc;
import io.grpc.ManagedChannel;

import java.util.Scanner;

public class UserAddServiceClient {

    private ManagedChannel channel = null;
    UserAddServiceGrpc.UserAddServiceBlockingStub clientStub = null;
    String host = null;
    int port = -1;

    public UserAddServiceClient (String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void initializeConnection (ManagedChannel channel) {
        System.out.println("Initializing Connecting to server at " + host + ":" +
                port);
        this.channel = channel;
        clientStub = UserAddServiceGrpc.newBlockingStub(this.channel);
    }
    public void closeConnection() {
        channel.shutdown();
    }

    public void processUserRequests(Scanner userInput) throws InterruptedException {
        System.out.println("\nEnter Username, First Name, Last Name, Email, Address, Role to create a new user :");
        String input[] = userInput.nextLine().trim().split(",");
        String userName = input[0];
        String firstName = input[1];
        String lastName = input[2];
        String email = input[3];
        String address = input[4];
        String role = input[5];
        System.out.println("Requesting server to add user " + userName);
        UserAddRequest request = UserAddRequest
                .newBuilder()
                .setUserName(userName)
                .setFirstName(firstName)
                .setLastName(lastName)
                .setEmail(email)
                .setAddress(address)
                .setRole(getRole(role))
                .setIsSentByPrimary(false)
                .build();
        StatusResponse response = clientStub.addUser(request);
        System.out.printf("Process Add User " + response.getStatus() + ". " + response.getMessage() + "\n");
        Thread.sleep(1000);
    }

    private Role getRole(String role) {
        if (role.equalsIgnoreCase("SELLER")) {
            return Role.SELLER;
        } else if (role.equalsIgnoreCase("BUYER")) {
            return Role.BUYER;
        } else if (role.equalsIgnoreCase("INVENTORY_CLERK")) {
            return Role.INVENTORY_CLERK;
        } else {
            return Role.ADMIN;
        }
    }
}
