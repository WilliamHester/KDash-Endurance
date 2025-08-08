import React, { useEffect, useState, useRef } from "react";
import VariableBox, { TextBox } from "./VariableBox";
import Row from "../base/Row";
import "./RaceOverviewPage.css";
import CarLapLog from "./CarLapLog";
import { QueryRealtimeTelemetryRequest } from "../live_telemetry_service_pb.js";
import { LiveTelemetryServiceClient } from "../live_telemetry_service_grpc_web_pb";


const fixedFormatter = (decimals) => (value) => {
  return value.toFixed(decimals);
}

const hourMinuteSecondFormatter = (value) => {
  const hours = String(Math.floor(value / 3600)).padStart(2, '0');
  const minutes = String(Math.floor((value % 3600) / 60)).padStart(2, '0');
  const seconds = String(Math.floor(value % 60)).padStart(2, '0');
  return `${hours}:${minutes}:${seconds}`;
};


export default function RaceOverviewPage({lapLog}) {
  const client = useRef(new LiveTelemetryServiceClient(`${location.origin}/api`)).current;
  const queryList = [
    {
      name: 'Session Time',
      query: 'SessionTime',
      getterAndSetter: useState(0),
      formatter: hourMinuteSecondFormatter,
    },
    {
      name: 'Time Remaining',
      query: 'SessionTimeRemain',
      getterAndSetter: useState(0),
      formatter: hourMinuteSecondFormatter,
    },
    // TODO: Fix this. It doesn't work when uncommented.
    { 
      name: 'Lap over lap fuel',
      query: 'LAP_DELTA(FuelLevel)',
      getterAndSetter: useState(-1),
      formatter: fixedFormatter(3),
    },
    {
      name: 'Position',
      query: 'PlayerCarPosition',
      getterAndSetter: useState(0),
    },
    {
      name: 'Lap',
      query: 'Lap',
      getterAndSetter: useState(0),
    },
    {
      name: 'Track Temp',
      query: 'TrackTempCrew',
      getterAndSetter: useState(0),
      formatter: (value) => `${value.toFixed(1)}°C`,
    },
    {
      name: 'Last Pit Lap',
      query: 'LastPitLap',
      getterAndSetter: useState(0),
    },
    {
      name: 'Stint Completed Laps',
      query: 'Lap - LastPitLap',
      getterAndSetter: useState(0),
    },
  ];

  useEffect(() => {
    const request = new QueryRealtimeTelemetryRequest();
    request.setSampleRateHz(1);
    request.setQueriesList(queryList.map(query => query.query));

    console.log(`Queries: ${request.getQueriesList()}`);

    const telemetryStream = client.queryRealtimeTelemetry(request, {});
    telemetryStream.on('data', response => {
      for (const [key, value] of response.getSparseQueryValuesMap().getEntryList()) {
        console.log(`${key}: ${value}`);
        queryList[key].getterAndSetter[1](value);
      }
    });

    return () => {
      telemetryStream.cancel();
    };
  }, [client]);

  return <div>
    <Row>
      { queryList.map(query => <TextBox key={query.name} title={query.name}>
        { query.formatter ? query.formatter(query.getterAndSetter[0]) : query.getterAndSetter[0] }
      </TextBox>) }
      {/* <TextBox title={'Sim Time'}>1:42 AM</TextBox> */}
      <TextBox title={'Stint Lap'}>6/24</TextBox>
      <TextBox title={'Track Precip'}>0&#37;</TextBox>
      <TextBox title={'Avg 5 Lap Fuel'}>4.323</TextBox>
      <TextBox title={'Repairs (Optional)'}>6:35 (8:12)</TextBox>
      <TextBox title={'Incidents (Current Driver)'}>31 (8)</TextBox>
      <TextBox title={'Session'}>Race</TextBox>
    </Row>
    <Row style={{'height': '500px'}}>
      <VariableBox title={'Laps'}>
        <CarLapLog entries={lapLog}></CarLapLog>
      </VariableBox>
    </Row>
  </div>;
};
