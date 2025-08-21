import React, { useEffect, useState, useRef } from "react";
import VariableBox, { TextBox } from "./VariableBox";
import Row from "../base/Row";
import "./RaceOverviewPage.css";
import CarLapLog from "./CarLapLog";
import { QueryRealtimeTelemetryRequest } from "../live_telemetry_service_pb.js";
import { LiveTelemetryServiceClient } from "../live_telemetry_service_grpc_web_pb";
import OtherCarsLapLog from "./OtherCarsLapLog";
import StintLogPage from "../stintlog/StintLogPage";


const hourMinuteSecondFormatter = (value) => {
  const hours = String(Math.floor(value / 3600)).padStart(2, '0');
  const minutes = String(Math.floor((value % 3600) / 60)).padStart(2, '0');
  const seconds = String(Math.floor(value % 60)).padStart(2, '0');
  return `${hours}:${minutes}:${seconds}`;
};

const timeOfDayFormatter = (value) => {
  let hoursOfDay = Math.floor(value / 3600);
  let amPm = 'AM';
  if (hoursOfDay > 12) {
    hoursOfDay -= 12;
    amPm = 'PM'
  }
  if (hoursOfDay == 0) {
    hoursOfDay = 12;
  }
  const hours = String(hoursOfDay).padStart(2, '0');
  const minutes = String(Math.floor((value % 3600) / 60)).padStart(2, '0');
  const seconds = String(Math.floor(value % 60)).padStart(2, '0');
  return `${hours}:${minutes}:${seconds} ${amPm}`;
};


export default function RaceOverviewPage({drivers, lapLog, stintLog, otherCarLapEntries}) {
  const client = useRef(new LiveTelemetryServiceClient(`${location.origin}/api`)).current;
  const [queryMap, setQueryMap] = useState(new Map());

  const getQueryValue = (query) => {
    if (queryMap.get(query) === undefined) {
      setQueryMap(map => {
        const newMap = new Map(map);
        newMap.set(query, 0);
        return newMap;
      });
      return 0;
    }
    return queryMap.get(query);
  }

  useEffect(() => {
    const request = new QueryRealtimeTelemetryRequest();
    request.setSampleRateHz(1);
    const queryList = Array.from(queryMap.keys());
    request.setQueriesList(queryList);

    const telemetryStream = client.queryRealtimeTelemetry(request, {});
    telemetryStream.on('data', response => {
      setQueryMap(map => {
        const newMap = new Map(map);
        for (const [key, value] of response.getSparseQueryValuesMap().getEntryList()) {
          newMap.set(queryList[key], value);
        };
        return newMap;
      });
    });

    return () => {
      telemetryStream.cancel();
    };
  }, [client]);

  return <div>
    <Row>
      <TextBox title="Sim Time">{ timeOfDayFormatter(getQueryValue('SessionTimeOfDay')) }</TextBox>
      <TextBox title="Time Remaining">{ hourMinuteSecondFormatter(getQueryValue('SessionTimeRemain')) }</TextBox>
      <TextBox title="Lap">{ getQueryValue('Lap') }</TextBox>
      <TextBox title="Track Temp">{ getQueryValue('TrackTempCrew').toFixed(1) }°C</TextBox>
      <TextBox title="Lap over lap fuel">{ getQueryValue('LAP_DELTA(FuelLevel)').toFixed(3) }</TextBox>
      <TextBox title="Track Precip">{ getQueryValue('TrackPrecip') }&#37;</TextBox>
      <TextBox title="Avg 5 Lap Fuel">{ getQueryValue('LAP_AVERAGE(LAP_DELTA(FuelLevel), 5)').toFixed(3) }</TextBox>
      <TextBox title="Repairs (Optional)">6:35 (8:12)</TextBox>
      <TextBox title="Incidents (Current Driver)">{ `${getQueryValue('PlayerCarTeamIncidentCount')} (${getQueryValue('PlayerCarDriverIncidentCount')})` }</TextBox>
      <TextBox title="Session">Race</TextBox>
    </Row>
    <Row style={{'height': '500px'}}>
      <VariableBox title={'Laps'}>
        <CarLapLog entries={lapLog}></CarLapLog>
      </VariableBox>
      <VariableBox title={'Stints'}>
        <StintLogPage entries={stintLog}></StintLogPage>
      </VariableBox>
      <VariableBox title="Other Team Laps">
        <OtherCarsLapLog entries={otherCarLapEntries} drivers={drivers}></OtherCarsLapLog>
      </VariableBox>
    </Row>
  </div>;
};
