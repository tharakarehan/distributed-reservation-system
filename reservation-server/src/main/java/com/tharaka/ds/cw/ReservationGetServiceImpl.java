package com.tharaka.ds.cw;

import common.tharaka.ds.cw.communication.grpc.generated.ReservationGetServiceGrpc;
import common.tharaka.ds.cw.communication.grpc.generated.ReservationSearchResponse;
import common.tharaka.ds.cw.communication.grpc.generated.UserRequest;
import io.grpc.stub.StreamObserver;

public class ReservationGetServiceImpl extends ReservationGetServiceGrpc.ReservationGetServiceImplBase {

    private final DataProviderImpl dataProvider;
    public ReservationGetServiceImpl(DataProviderImpl dataProvider) {
        this.dataProvider = dataProvider;
    }

    @Override
    public void getReservations(UserRequest request, StreamObserver<ReservationSearchResponse> responseObserver) {
        System.out.println("Request for getReservations received..");
        String buyerId = request.getUserName();
        ReservationSearchResponse response = ReservationSearchResponse
                .newBuilder()
                .addAllReservations(dataProvider.getReservationsByUserName(buyerId))
                .build();
        System.out.println("Responding with all reservations");
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
