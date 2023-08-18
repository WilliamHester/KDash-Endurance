import React, { useEffect, useState } from "react";
import { Outlet } from "react-router";
import { Link, NavLink } from "react-router-dom";
import { ConnectRequest, LiveTelemetryEvent } from "./live_telemetry_service_pb.js";
import { LiveTelemetryServiceClient } from "./live_telemetry_service_grpc_web_pb.js";
import "./base.css";

function App() {
  return (
    <div className="column">
      <header className="header">
        <div className="wrapper site-header__wrapper">
          <Link className="no-style-link" to="/">
            <span className="website-name">Stintlytics</span>
          </Link>
        </div>
      </header>

      <div className="row">
        <div className="hamburgerMenu">
          <NavLink className="no-style-link hamburgerMenuItem" to="/laps">Lap Records</NavLink>
          <NavLink className="no-style-link hamburgerMenuItem" to="/otherlaps">Other Cars' Laps</NavLink>
          <NavLink className="no-style-link hamburgerMenuItem" to="/gaps">Current Gaps</NavLink>
          <NavLink className="no-style-link hamburgerMenuItem" to="/gapchart">Gap Chart</NavLink>
        </div>

        <div className="column content">
          <Outlet />
        </div>
      </div>
    </div>
  );
};

export default App;