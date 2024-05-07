package com.tharaka.ds.cw;

import com.tharaka.ds.cw.sycnhronization.DistributedLock;
import com.tharaka.ds.cw.sycnhronization.DistributedTx;
import com.tharaka.ds.cw.sycnhronization.DistributedTxCoordinator;
import com.tharaka.ds.cw.sycnhronization.DistributedTxParticipant;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReservationServer {

    private final int serverPort;
    private final DistributedLock leaderLock;
    private final AtomicBoolean isLeader = new AtomicBoolean(false);
    private byte[] leaderData;
    private DistributedTx transactionItemAdd;
    private DistributedTx transactionItemUpdate;
    private DistributedTx transactionItemDelete;
    private DistributedTx transactionReserve;
    private DistributedTx transactionUserAdd;
    private final ItemAddServiceImpl itemAddService;
    private final ItemUpdateServiceImpl itemUpdateService;
    private final ItemDeleteServiceImpl itemDeleteService;
    private final ItemSearchServiceImpl itemSearchService;
    private final ReserveServiceImpl reserveService;
    private final ReservationGetServiceImpl reservationGetService;
    private final UserAddServiceImpl userAddService;

    public ReservationServer(String host, int port) throws IOException, InterruptedException, KeeperException {
        this.serverPort = port;
        leaderLock = new DistributedLock("ReservationServerCluster", buildServerData(host, port));
        DataProviderImpl dataProvider = new DataProviderImpl();
        itemAddService = new ItemAddServiceImpl(this, dataProvider);
        itemUpdateService = new ItemUpdateServiceImpl(this, dataProvider);
        itemDeleteService = new ItemDeleteServiceImpl(this, dataProvider);
        itemSearchService = new ItemSearchServiceImpl(dataProvider);
        reserveService = new ReserveServiceImpl(this, dataProvider);
        reservationGetService = new ReservationGetServiceImpl(dataProvider);
        userAddService = new UserAddServiceImpl(this, dataProvider);
        transactionItemAdd = new DistributedTxParticipant(itemAddService);
        transactionItemUpdate = new DistributedTxParticipant(itemUpdateService);
        transactionItemDelete = new DistributedTxParticipant(itemDeleteService);
        transactionReserve = new DistributedTxParticipant(reserveService);
        transactionUserAdd = new DistributedTxParticipant(userAddService);
    }
    public void startServer() throws IOException, InterruptedException, KeeperException {
        Server server = ServerBuilder
                .forPort(serverPort)
                .addService(itemAddService)
                .addService(itemUpdateService)
                .addService(itemDeleteService)
                .addService(itemSearchService)
                .addService(reserveService)
                .addService(reservationGetService)
                .addService(userAddService)
                .build();
        server.start();
        System.out.println("ReservationServer Started and ready to accept requests on port " + serverPort);

        tryToBeLeader();
        server.awaitTermination();
    }

    private void tryToBeLeader() throws KeeperException, InterruptedException {
        Thread leaderCampaignThread = new Thread(new LeaderCampaignThread());
        leaderCampaignThread.start();
    }

    public synchronized String[] getCurrentLeaderData() {
        return new String(leaderData).split(":");
    }

    public List<String[]> getOthersData() throws KeeperException, InterruptedException {
        List<String[]> result = new ArrayList<>();
        List<byte[]> othersData = leaderLock.getOthersData();
        for (byte[] data : othersData) {
            String[] dataStrings = new String(data).split(":");
            result.add(dataStrings);
        }
        return result;
    }

    private void beTheLeader() {
        System.out.println("I got the leader lock. Now acting as primary");
        isLeader.set(true);
        transactionItemAdd = new DistributedTxCoordinator(itemAddService);
        transactionItemUpdate = new DistributedTxCoordinator(itemUpdateService);
        transactionItemDelete = new DistributedTxCoordinator(itemDeleteService);
        transactionReserve = new DistributedTxCoordinator(reserveService);
        transactionUserAdd = new DistributedTxCoordinator(userAddService);

    }

    public static void main (String[] args) throws Exception{
        if (args.length != 1) {
            System.out.println("Usage executable-name <port>");
        }

        int serverPort = Integer.parseInt(args[0]);
        DistributedLock.setZooKeeperURL("localhost:2181");
        DistributedTx.setZooKeeperURL("localhost:2181");

        ReservationServer server = new ReservationServer("localhost", serverPort);
        server.startServer();
    }

    class LeaderCampaignThread implements Runnable {
        private byte[] currentLeaderData = null;

        @Override
        public void run() {
            System.out.println("Starting the leader Campaign");
            try {
                boolean leader = leaderLock.tryAcquireLock();
                while (!leader) {
                    byte[] leaderData = leaderLock.getLockHolderData();
                    if (currentLeaderData != leaderData) {
                        currentLeaderData = leaderData;
                        setCurrentLeaderData(currentLeaderData);
                    }
                    Thread.sleep(10000);
                    leader = leaderLock.tryAcquireLock();
                }
                beTheLeader();
                currentLeaderData = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static String buildServerData(String IP, int port) {
        StringBuilder builder = new StringBuilder();
        builder.append(IP).append(":").append(port);
        return builder.toString();
    }

    public boolean isLeader() {
        return isLeader.get();
    }

    private synchronized void setCurrentLeaderData(byte[] leaderData) {
        this.leaderData = leaderData;
    }

    public DistributedTx getTransactionItemAdd() {
        return transactionItemAdd;
    }

    public DistributedTx getTransactionItemUpdate() {
        return transactionItemUpdate;
    }

    public DistributedTx getTransactionItemDelete() {
        return transactionItemDelete;
    }

    public DistributedTx getTransactionReserve() {
        return transactionReserve;
    }

    public DistributedTx getTransactionUserAdd() {
        return transactionUserAdd;
    }
}
