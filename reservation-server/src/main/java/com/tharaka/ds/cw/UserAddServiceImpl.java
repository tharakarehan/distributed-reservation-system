package com.tharaka.ds.cw;

import com.tharaka.ds.cw.sycnhronization.DistributedTxCoordinator;
import com.tharaka.ds.cw.sycnhronization.DistributedTxListener;
import com.tharaka.ds.cw.sycnhronization.DistributedTxParticipant;
import common.tharaka.ds.cw.communication.grpc.generated.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.List;
import java.util.UUID;

public class UserAddServiceImpl extends UserAddServiceGrpc.UserAddServiceImplBase implements DistributedTxListener {

    UserAddServiceGrpc.UserAddServiceBlockingStub clientStub = null;
    private ManagedChannel channel = null;
    private final ReservationServer server;
    private final DataProviderImpl dataProvider;
    private Status status = Status.FAILURE;
    private String statusMessage = "";
    private AbstractMap.SimpleEntry<String, UserAddRequest> tempDataHolder;
    public UserAddServiceImpl(ReservationServer reservationServer, DataProviderImpl dataProvider) {
        this.server = reservationServer;
        this.dataProvider = dataProvider;
    }

    private StatusResponse callServer(UserAddRequest userAddRequest, boolean isSentByPrimary,
                                      String IPAddress, int port) {
        System.out.println("Call Server " + IPAddress + ":" + port);
        channel = ManagedChannelBuilder.forAddress(IPAddress, port)
                .usePlaintext()
                .build();
        clientStub = UserAddServiceGrpc.newBlockingStub(channel);
        UserAddRequest request = userAddRequest.toBuilder()
                .setIsSentByPrimary(isSentByPrimary)
                .build();
        StatusResponse response = clientStub.addUser(request);
        return response;
    }

    private StatusResponse callPrimary(UserAddRequest userAddRequest) {
        System.out.println("Calling Primary server");
        String[] currentLeaderData = server.getCurrentLeaderData();
        String IPAddress = currentLeaderData[0];
        int port = Integer.parseInt(currentLeaderData[1]);
        return callServer(userAddRequest, false, IPAddress, port);
    }
    private void updateSecondaryServers(UserAddRequest userAddRequest) throws KeeperException, InterruptedException {
        System.out.println("Updating secondary servers");
        List<String[]> othersData = server.getOthersData();
        for (String[] data : othersData) {
            String IPAddress = data[0];
            int port = Integer.parseInt(data[1]);
            callServer(userAddRequest, true, IPAddress, port);
        }
    }

    private void startDistributedTx(String userName, UserAddRequest userAddRequest) {
        try {
            server.getTransactionUserAdd().start(userName, String.valueOf(UUID.randomUUID()));
            tempDataHolder = new AbstractMap.SimpleEntry<>(userName, userAddRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGlobalCommit() {
        persistUser();
    }

    @Override
    public void onGlobalAbort() {
        tempDataHolder = null;
        status = Status.FAILURE;
        System.out.println("Transaction Aborted by the Coordinator");
    }

    @Override
    public void addUser(UserAddRequest request, StreamObserver<StatusResponse> responseObserver) {
        if (server.isLeader()) {
            // Act as primary
            try {
                System.out.println("Adding User as Primary");
                startDistributedTx(request.getUserName(), request);
                updateSecondaryServers(request);
                System.out.println("going to perform");
                if (checkEligibility(request)){
                    ((DistributedTxCoordinator) server.getTransactionUserAdd()).perform();
                } else {
                    ((DistributedTxCoordinator) server.getTransactionUserAdd()).sendGlobalAbort();
                }
            } catch (Exception e) {
                System.out.println("Error while adding a user" + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // Act As Secondary
            if (request.getIsSentByPrimary()) {
                System.out.println("Adding new user on secondary, on Primary's command");
                startDistributedTx(request.getUserName(), request);
                if (checkEligibility(request)) {
                    ((DistributedTxParticipant) server.getTransactionUserAdd()).voteCommit();
                } else {
                    ((DistributedTxParticipant) server.getTransactionUserAdd()).voteAbort();
                }
            } else {
                StatusResponse response = callPrimary(request);
                if (response.getStatus() == Status.SUCCESS) {
                    status = Status.SUCCESS;
                }
            }
        }
        StatusResponse response = StatusResponse
                .newBuilder()
                .setStatus(status)
                .setMessage(statusMessage)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private boolean checkEligibility(UserAddRequest request) {
        if (dataProvider.isUserExist(request.getUserName())) {
            statusMessage = "UserName exists";
            status = Status.FAILURE;
            return false;
        }
        return true;
    }

    private void persistUser() {
        System.out.println("Persisting User");
        if (tempDataHolder != null) {
            UserAddRequest request = tempDataHolder.getValue();
            dataProvider.addUser(request);
            System.out.println("User " + request.getUserName() + " of Role " + request.getRole() + " Added & committed");
            status = Status.SUCCESS;
            statusMessage = "User Added Successfully";
            tempDataHolder = null;
        }
    }
}
