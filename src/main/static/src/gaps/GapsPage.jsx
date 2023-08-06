import React, { useEffect, useState } from "react";
import { ConnectRequest } from "../live_telemetry_service_pb.js";
import { LiveTelemetryServiceClient } from "../live_telemetry_service_grpc_web_pb.js";

export default function GapsPage() {
  const [gapEntries, setGapEntries] = useState([]);

  useEffect(() => {
    const liveTelemetryServiceClient = new LiveTelemetryServiceClient('http://localhost:8000/api');
    const request = new ConnectRequest();
    const entries = [];
    const stream = liveTelemetryServiceClient.monitorCurrentGaps(request, {}, (err, resp) => {
        console.log("went here");
        console.log(err, resp);
    });
    stream.on('data', response => {
      setGapEntries([...response.getGapsList()]);
    });
    stream.on('status', status => {
      console.log(status);
    });
    stream.on('end', end => {
      console.log('Stream end');
    });
  }, []);

  return (
    <div>
      { gapEntries.map((gap) => <div>{ gap.getGap().toFixed(3) }</div>)}
    </div>
  );
}
