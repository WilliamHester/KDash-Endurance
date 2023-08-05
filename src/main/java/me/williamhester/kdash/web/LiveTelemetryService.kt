package me.williamhester.kdash.web

import io.grpc.stub.StreamObserver
import me.williamhester.kdash.enduranceweb.proto.ConnectRequest
import me.williamhester.kdash.enduranceweb.proto.LapLog
import me.williamhester.kdash.enduranceweb.proto.LiveTelemetryEvent
import me.williamhester.kdash.enduranceweb.proto.LiveTelemetryServiceGrpc.LiveTelemetryServiceImplBase

class LiveTelemetryService : LiveTelemetryServiceImplBase() {
  override fun connect(
    request: ConnectRequest,
    responseObserver: StreamObserver<LiveTelemetryEvent>
  ) {
    responseObserver.onNext(LiveTelemetryEvent.newBuilder().setLapLog(LapLog.newBuilder().setLapNum(1).build()).build())
    responseObserver.onNext(LiveTelemetryEvent.newBuilder().setLapLog(LapLog.newBuilder().setLapNum(2).build()).build())
    responseObserver.onNext(LiveTelemetryEvent.newBuilder().setLapLog(LapLog.newBuilder().setLapNum(3).build()).build())
    responseObserver.onCompleted()
  }
}
