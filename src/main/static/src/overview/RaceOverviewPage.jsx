import React from "react";
import VariableBox from "./VariableBox";
import Row from "../base/Row";
import "./RaceOverviewPage.css";
import LapLogPage from "../laplog/LapLogPage";

export default function RaceOverviewPage({lapLog}) {
  return <div>
    <Row>
      <VariableBox title={'Session Time'}>1:42 AM</VariableBox>
      <VariableBox title={'Time Remaining'}>11:33:36</VariableBox>
      <VariableBox title={'Position'}>14</VariableBox>
      <VariableBox title={'Lap'}>172</VariableBox>
      <VariableBox title={'Stint Lap'}>6/24</VariableBox>
      <VariableBox title={'Track Temp'}>21&deg;C</VariableBox>
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
