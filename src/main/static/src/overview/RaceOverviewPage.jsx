import React, { useEffect, useState, useRef } from "react";
import VariableBox, { TextBox } from "./VariableBox";
import Row from "../base/Row";
import "./RaceOverviewPage.css";
import CarLapLog from "./CarLapLog";
import { QueryRealtimeTelemetryRequest, QueryResult } from "../live_telemetry_service_pb.js";
import { LiveTelemetryServiceClient } from "../live_telemetry_service_grpc_web_pb";
import OtherCarsLapLog from "./OtherCarsLapLog";
import StintLogPage from "../stintlog/StintLogPage";
import OtherCarStintLogPage from "../othercarstintlog/OtherCarStintLogPage.jsx";


const hourMinuteSecondFormatter = (value) => {
  const hours = String(Math.floor(value / 3600)).padStart(2, '0');
  const minutes = String(Math.floor((value % 3600) / 60)).padStart(2, '0');
  const seconds = String(Math.floor(value % 60)).padStart(2, '0');
  return `${hours}:${minutes}:${seconds}`;
};

const minuteSecondFormatter = (value) => {
  const minutes = String(Math.floor(value / 60));
  const seconds = String(Math.floor(value % 60).toFixed(1)).padStart(4, '0');
  return `${minutes}:${seconds}`;
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


export default function RaceOverviewPage({session, drivers, lapLog, stintLog, otherCarLapEntries, otherCarStintEntries}) {
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

  const getQueryListValue = (query) => {
    if (queryMap.get(query) === undefined) {
      setQueryMap(map => {
        const newMap = new Map(map);
        newMap.set(query, []);
        return newMap;
      });
      return [];
    }
    return queryMap.get(query);
  }

  useEffect(() => {
    const request = new QueryRealtimeTelemetryRequest();
    request.setSessionIdentifier(session);
    request.setSampleRateHz(1);
    const queryList = Array.from(queryMap.keys());
    request.setQueriesList(queryList);

    const telemetryStream = client.queryRealtimeTelemetry(request, {});
    telemetryStream.on('data', response => {
      setQueryMap(map => {
        const newMap = new Map(map);
        for (const [key, value] of response.getSparseQueryValuesMap().entries()) {
          switch (value.getValueCase()) {
            case QueryResult.ValueCase.SCALAR:
              newMap.set(queryList[key], value.getScalar());
              break;
            case QueryResult.ValueCase.LIST:
              newMap.set(queryList[key], value.getList().getValuesList());
              break;
            default:
              continue;
          }
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
      <TextBox title="Lap Fuel">{ getQueryValue('DECREASING_SUM(FuelLevel, 1)').toFixed(3) }</TextBox>
      <TextBox title="Avg 5 Lap Fuel">{ getQueryValue('DECREASING_SUM(FuelLevel, 5) / 5').toFixed(3) }</TextBox>
      <TextBox title="Track Precip">{ getQueryValue('TrackPrecip') }&#37;</TextBox>
      <TextBox title="Fuel Target for + 1 Lap">{ getQueryValue('(FuelLevel - 1) / CEILING(((FuelLevel - 1) / DECREASING_SUM(FuelLevel, 1)))').toFixed(3) }</TextBox>
      {/* <TextBox title="Laps Remaining">{ getQueryValue('(Lap + LapDistPLastPitLap') }</TextBox> */}
      <TextBox title="Repairs (Optional)">{ minuteSecondFormatter(getQueryValue('PitReqRepairRemaining')) } ({ minuteSecondFormatter(getQueryValue('PitOptRepairRemaining')) })</TextBox>
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
      <VariableBox title="Other Team Stints">
        <OtherCarStintLogPage entries={otherCarStintEntries} drivers={drivers}></OtherCarStintLogPage>
      </VariableBox>
      <VariableBox>
        { getQueryListValue('CarIdxEstTime').map((value, i) => <div key={i}>{value.toFixed(2)}<br/></div>) }
      </VariableBox>
    </Row>
  </div>;
};
