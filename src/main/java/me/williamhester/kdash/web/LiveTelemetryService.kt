package me.williamhester.kdash.web

import io.grpc.stub.ClientCallStreamObserver
import io.grpc.stub.StreamObserver
import me.williamhester.kdash.api.IRacingLoggedDataReader
import me.williamhester.kdash.enduranceweb.proto.ConnectRequest
import me.williamhester.kdash.enduranceweb.proto.LapEntry
import me.williamhester.kdash.enduranceweb.proto.LiveTelemetryServiceGrpc.LiveTelemetryServiceImplBase
import me.williamhester.kdash.monitors.DriverCarLapMonitor
import me.williamhester.kdash.monitors.RelativeMonitor
import java.nio.file.Paths

class LiveTelemetryService : LiveTelemetryServiceImplBase(), Runnable {

  private lateinit var lapMonitor: DriverCarLapMonitor

  override fun run() {
    val iRacingDataReader = IRacingLoggedDataReader(Paths.get("/Users/williamhester/Downloads/livedata.ibt"))
    val relativeMonitor = RelativeMonitor(iRacingDataReader.headers)
    lapMonitor = DriverCarLapMonitor(iRacingDataReader, relativeMonitor)

    for (varBuf in iRacingDataReader) {
      relativeMonitor.process(varBuf)
      lapMonitor.process(varBuf)
    }
  }

  override fun monitorDriverLaps(
    request: ConnectRequest,
    responseObserver: StreamObserver<LapEntry>
  ) {
    // 1. Emit all laps we've seen so far
    for (lap in lapMonitor.logEntries) {
      responseObserver.onNext(lap.toLapEntry())
    }
//    responseObserver.onCompleted()
    // 2. Hand off the response observer
  }
}
