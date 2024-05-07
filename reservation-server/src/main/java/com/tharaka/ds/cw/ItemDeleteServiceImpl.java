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

public class ItemDeleteServiceImpl extends ItemDeleteServiceGrpc.ItemDeleteServiceImplBase implements DistributedTxListener {

    ItemDeleteServiceGrpc.ItemDeleteServiceBlockingStub clientStub = null;
    private ManagedChannel channel = null;
    private final ReservationServer server;
    private final DataProviderImpl dataProvider;
    private Status status = Status.FAILURE;
    private String statusMessage = "";
    private AbstractMap.SimpleEntry<String, ItemDeleteRequest> tempDataHolder;
    public ItemDeleteServiceImpl(ReservationServer reservationServer, DataProviderImpl dataProvider) {
        this.server = reservationServer;
        this.dataProvider = dataProvider;
    }

    private StatusResponse callServer(ItemDeleteRequest itemDeleteRequest, boolean isSentByPrimary,
                                      String IPAddress, int port) {
        System.out.println("Call Server " + IPAddress + ":" + port);
        channel = ManagedChannelBuilder.forAddress(IPAddress, port)
                .usePlaintext()
                .build();
        clientStub = ItemDeleteServiceGrpc.newBlockingStub(channel);
        ItemDeleteRequest request = itemDeleteRequest.toBuilder()
                .setIsSentByPrimary(isSentByPrimary)
                .build();
        StatusResponse response = clientStub.deleteItem(request);
        return response;
    }

    private StatusResponse callPrimary(ItemDeleteRequest itemDeleteRequest) {
        System.out.println("Calling Primary server");
        String[] currentLeaderData = server.getCurrentLeaderData();
        String IPAddress = currentLeaderData[0];
        int port = Integer.parseInt(currentLeaderData[1]);
        return callServer(itemDeleteRequest, false, IPAddress, port);
    }
    private void updateSecondaryServers(ItemDeleteRequest itemDeleteRequest) throws KeeperException, InterruptedException {
        System.out.println("Updating secondary servers");
        List<String[]> othersData = server.getOthersData();
        for (String[] data : othersData) {
            String IPAddress = data[0];
            int port = Integer.parseInt(data[1]);
            callServer(itemDeleteRequest, true, IPAddress, port);
        }
    }

    private void startDistributedTx(String itemId, ItemDeleteRequest itemDeleteRequest) {
        try {
            server.getTransactionItemDelete().start("delete"+itemId, String.valueOf(UUID.randomUUID()));
            tempDataHolder = new AbstractMap.SimpleEntry<>(itemId, itemDeleteRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGlobalCommit() {
        deleteItem();
    }

    @Override
    public void onGlobalAbort() {
        tempDataHolder = null;
        status = Status.FAILURE;
        System.out.println("Transaction Aborted by the Coordinator");
    }

    @Override
    public void deleteItem(ItemDeleteRequest request, StreamObserver<StatusResponse> responseObserver) {
        if (server.isLeader()) {
            // Act as primary
            try {
                System.out.println("Deleting Item as Primary");
                startDistributedTx(request.getItemId(), request);
                updateSecondaryServers(request);
                System.out.println("going to perform");
                if (checkEligibility(request)){
                    ((DistributedTxCoordinator) server.getTransactionItemDelete()).perform();
                } else {
                    ((DistributedTxCoordinator) server.getTransactionItemDelete()).sendGlobalAbort();
                }
            } catch (Exception e) {
                System.out.println("Error while deleting a new item" + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // Act As Secondary
            if (request.getIsSentByPrimary()) {
                System.out.println("Deleting a new item on secondary, on Primary's command");
                startDistributedTx(request.getItemId(), request);
                if (checkEligibility(request)) {
                    ((DistributedTxParticipant) server.getTransactionItemDelete()).voteCommit();
                } else {
                    ((DistributedTxParticipant) server.getTransactionItemDelete()).voteAbort();
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

    private boolean checkEligibility(ItemDeleteRequest request) {
        if (!dataProvider.isUserExist(request.getSellerName())) {
            statusMessage = "Seller does not exist";
            status = Status.FAILURE;
            return false;
        }
        if (!dataProvider.isItemExist(request.getItemId())) {
            statusMessage = "Item does not exist";
            status = Status.FAILURE;
            return false;
        }
        return true;
    }

    private void deleteItem() {
        if (tempDataHolder != null) {
            ItemDeleteRequest request = tempDataHolder.getValue();
            dataProvider.deleteItem(request.getItemId());
            System.out.println("Item " + request.getItemId() + " Deleted & committed");
            status = Status.SUCCESS;
            statusMessage = "Item Deleted Successfully";
            tempDataHolder = null;
        }
    }
}
