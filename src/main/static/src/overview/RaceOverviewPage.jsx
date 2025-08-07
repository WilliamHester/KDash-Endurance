import React, { useEffect, useState, useRef } from "react";
import VariableBox from "./VariableBox";
import Row from "../base/Row";
import "./RaceOverviewPage.css";
import LapLogPage from "../laplog/LapLogPage";
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
      // formatter: (value) => `${value.toFixed(1)}°C`,
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

  // console.log(queryList);

  return <div>
    <Row>
      { queryList.map(query => <VariableBox key={query.name} title={query.name}>
        { query.formatter ? query.formatter(query.getterAndSetter[0]) : query.getterAndSetter[0] }
      </VariableBox>) }
      {/* <VariableBox title={'Sim Time'}>1:42 AM</VariableBox> */}
      <VariableBox title={'Stint Lap'}>6/24</VariableBox>
      <VariableBox title={'Track Precip'}>0&#37;</VariableBox>
      <VariableBox title={'Avg 5 Lap Fuel'}>4.323</VariableBox>
      <VariableBox title={'Repairs (Optional)'}>6:35 (8:12)</VariableBox>
      <VariableBox title={'Incidents (Current Driver)'}>31 (8)</VariableBox>
      <VariableBox title={'Session'}>Race</VariableBox>
    </Row>
    <Row style={{'height': '500px'}}>
      <VariableBox title={'Laps'}>
        <LapLogPage entries={lapLog}></LapLogPage>
      </VariableBox>
    </Row>
  </div>;
};
