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

public class ItemAddServiceImpl extends ItemAddServiceGrpc.ItemAddServiceImplBase implements DistributedTxListener {
    ItemAddServiceGrpc.ItemAddServiceBlockingStub clientStub = null;
    private ManagedChannel channel = null;
    private final ReservationServer server;
    private final DataProviderImpl dataProvider;
    private Status status = Status.FAILURE;
    private String statusMessage = "";
    private AbstractMap.SimpleEntry<String, ItemAddRequest> tempDataHolder;
    public ItemAddServiceImpl(ReservationServer reservationServer, DataProviderImpl dataProvider) {
        this.server = reservationServer;
        this.dataProvider = dataProvider;
    }

    private StatusResponse callServer(ItemAddRequest itemAddRequest, boolean isSentByPrimary,
                                      String IPAddress, int port) {
        System.out.println("Call Server " + IPAddress + ":" + port);
        channel = ManagedChannelBuilder.forAddress(IPAddress, port)
                .usePlaintext()
                .build();
        clientStub = ItemAddServiceGrpc.newBlockingStub(channel);
        ItemAddRequest request = itemAddRequest.toBuilder()
                .setIsSentByPrimary(isSentByPrimary)
                .build();
        StatusResponse response = clientStub.addItem(request);
        return response;
    }

    private StatusResponse callPrimary(ItemAddRequest itemAddRequest) {
        System.out.println("Calling Primary server");
        String[] currentLeaderData = server.getCurrentLeaderData();
        String IPAddress = currentLeaderData[0];
        int port = Integer.parseInt(currentLeaderData[1]);
        return callServer(itemAddRequest, false, IPAddress, port);
    }
    private void updateSecondaryServers(ItemAddRequest itemAddRequest) throws KeeperException, InterruptedException {
        System.out.println("Updating secondary servers");
        List<String[]> othersData = server.getOthersData();
        for (String[] data : othersData) {
            String IPAddress = data[0];
            int port = Integer.parseInt(data[1]);
            callServer(itemAddRequest, true, IPAddress, port);
        }
    }

    private void startDistributedTx(String itemId, ItemAddRequest itemAddRequest) {
        try {
            server.getTransactionItemAdd().start(itemId, String.valueOf(UUID.randomUUID()));
            tempDataHolder = new AbstractMap.SimpleEntry<>(itemId, itemAddRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGlobalCommit() {
        persistItem();
    }

    @Override
    public void onGlobalAbort() {
        tempDataHolder = null;
        status = Status.FAILURE;
        System.out.println("Transaction Aborted by the Coordinator");
    }

    @Override
    public void addItem(ItemAddRequest request, StreamObserver<StatusResponse> responseObserver) {
        if (server.isLeader()) {
            // Act as primary
            try {
                System.out.println("Adding Item as Primary");
                startDistributedTx(request.getItemId(), request);
                updateSecondaryServers(request);
                System.out.println("going to perform");
                if (checkEligibility(request)){
                    ((DistributedTxCoordinator) server.getTransactionItemAdd()).perform();
                } else {
                    ((DistributedTxCoordinator) server.getTransactionItemAdd()).sendGlobalAbort();
                }
            } catch (Exception e) {
                System.out.println("Error while adding a new item" + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // Act As Secondary
            if (request.getIsSentByPrimary()) {
                System.out.println("Adding new item on secondary, on Primary's command");
                startDistributedTx(request.getItemId(), request);
                if (checkEligibility(request)) {
                    ((DistributedTxParticipant) server.getTransactionItemAdd()).voteCommit();
                } else {
                    ((DistributedTxParticipant) server.getTransactionItemAdd()).voteAbort();
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

    private boolean checkEligibility(ItemAddRequest request) {
        if (!dataProvider.isUserExist(request.getSellerName())) {
            statusMessage = "Seller does not exist";
            status = Status.FAILURE;
            return false;
        }
        User seller = dataProvider.getUser(request.getSellerName());
        if ((seller.getRole() == Role.INVENTORY_CLERK && request.getType() != Type.NEW_ARRIVAL)
        || (seller.getRole() != Role.INVENTORY_CLERK && request.getType() == Type.NEW_ARRIVAL)) {
            statusMessage = "User has conflicting role";
            status = Status.FAILURE;
            return false;
        }
        return true;
    }

    private void persistItem() {
        if (tempDataHolder != null) {
            ItemAddRequest request = tempDataHolder.getValue();
            dataProvider.addItem(request);
            System.out.println("Item " + request.getItemName() + " of type " + request.getType() + " Added & committed");
            status = Status.SUCCESS;
            statusMessage = "Item Added Successfully";
            tempDataHolder = null;
        }
    }
}
