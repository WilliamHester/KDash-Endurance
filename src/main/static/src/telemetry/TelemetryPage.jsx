import React, { useState, useEffect, useRef } from "react";
import PropTypes from "prop-types";
import "./TelemetryPage.css";
import Chart2 from "../charts/Chart2";
import { ChartContainer } from "../charts/ChartContainer";
import { QueryTelemetryRequest } from "../live_telemetry_service_pb.js";
import { LiveTelemetryServiceClient } from "../live_telemetry_service_grpc_web_pb.js";


const MAX_DATA_POINTS = 3000;

const ALLOWED_HZ = [0.5, 1, 3, 6, 12, 30, 60];

const coerceToAllowedHz = (goal) => ALLOWED_HZ.reduce(function (prev, curr) {
  return (Math.abs(curr - goal) < Math.abs(prev - goal) ? curr : prev);
});


TelemetryPage.propTypes = {
  telemetryData: PropTypes.array.isRequired,
  dataRange: PropTypes.object.isRequired,
  dataWindow: PropTypes.array.isRequired,
  setDataWindow: PropTypes.func.isRequired,
};

const telemetryQueries = [
  'FuelLevel',
  'LAP_DELTA(FuelLevel)',
  'TrackTempCrew',
  'Speed',
  'LAP_DELTA(Speed)',
];

export default function TelemetryPage() {
  const [telemetryData, setTelemetryData] = useState([]);
  const [sampleRateHz, setSampleRateHz] = useState(8);
  const [dataRange, setDataRange] = useState({
    min: 0,
    max: 300,
  });
  const [dataWindow, setDataWindow] = useState([-1, 0]);
  const [counter, setCounter] = useState(0);

  const shouldClearData = useRef(false);
  const telemetryDataBuffer = useRef([]);
  const dataRangeBuffer = useRef(dataRange);
  const timeoutRef = useRef(null);

  const client = useRef(new LiveTelemetryServiceClient(`${location.origin}/api`)).current;

  useEffect(() => {
    const request = new QueryTelemetryRequest();
    request.setSampleRateHz(sampleRateHz);
    request.setMinSessionTime(dataWindow[0]);
    request.setMaxSessionTime(dataWindow[1]);
    request.setQueriesList(telemetryQueries);

    console.log('Querying for new data. dataStart: %s, dataEnd: %s, sampleRateHz: %s', dataWindow[0], dataWindow[1], sampleRateHz);
    const telemetryStream = client.queryTelemetry(request, {});
    telemetryStream.on('data', response => {
      if (response.hasDataRanges()) {
        const dataRanges = response.getDataRanges();
        const updatedDataRange = {
          min: dataRanges.getSessionTime().getMin(),
          max: dataRanges.getSessionTime().getMax(),
        };
        dataRangeBuffer.current = updatedDataRange;
      } else {
        const telemetryData = response.getData();
        telemetryDataBuffer.current.push(telemetryData);
        const sessionTime = dataRangeBuffer.current;
        dataRangeBuffer.current.max = Math.max(sessionTime.max, telemetryData.getSessionTime());
      }
    });

    return () => {
      telemetryStream.cancel();
      telemetryDataBuffer.current = [];
      shouldClearData.current = true;
    };
  }, [client, counter]);
  
  useEffect(() => {
    if (telemetryData.length === 0) {
      // Haven't loaded any telemetry data yet. Nothing to do.
      return;
    }
    const min = telemetryData[0];
    const max = telemetryData[telemetryData.length - 1];

    const newHz = coerceToAllowedHz(1000 / (dataWindow[1] - dataWindow[0]));

    if (dataWindow[0] < min.getSessionTime()
      || dataWindow[1] > max.getSessionTime()
      || sampleRateHz != newHz) {
      timeoutRef.current = setTimeout(() => {
        setCounter(currentCount => currentCount + 1);
        setSampleRateHz(newHz);
      }, 500);
      return () => {
        clearTimeout(timeoutRef.current);
      };
    }
  }, [dataWindow]);

  useEffect(() => {
    const intervalId = setInterval(() => {
      if (telemetryDataBuffer.current.length > 0) {
        setTelemetryData(prev => {
          let dataToPrepend;
          if (shouldClearData.current) {
            dataToPrepend = [];
            shouldClearData.current = false;
          } else {
            dataToPrepend = prev;
          }
          const updated = [...dataToPrepend, ...telemetryDataBuffer.current].slice(-MAX_DATA_POINTS);
          telemetryDataBuffer.current = [];
          // TODO: Fix this. I think this needs a ref or something.
          if (dataWindow[0] == -1 && dataWindow[1] == 0 && updated.length > 0) {
            setDataWindow([updated[0].getSessionTime(), updated[updated.length - 1].getSessionTime()]);
          }
          return updated;
        });
        setDataRange(dataRangeBuffer.current);
      }
    }, 250);

    return () => clearInterval(intervalId);
  }, [dataWindow]);

  const chartsData = [];
  if (telemetryData.length > 0) {
    const numCharts = telemetryData[0].getQueryValuesList().length;
    for (let i = 0; i < numCharts; i++) {
      chartsData.push(telemetryData.map(data => data.getQueryValuesList()[i]));
    }
  }
  const drivers = [{driverName: 'Hardcoded Driver Name'}];

  const xAxis = {
    sessionTimes: telemetryData.map(data => data.getSessionTime()),
    driverDistances: telemetryData.map(data => data.getDriverDistance()),
  };

  return (
    <ChartContainer dataRange={dataRange} dataWindow={dataWindow} setDataWindow={setDataWindow} >
      {
        chartsData.map((chartData, index) => <Chart2 key={index} title={`Chart ${index}`} xAxis={xAxis} lines={[chartData]} drivers={drivers}></Chart2>)
      }
    </ChartContainer>
  )
}
