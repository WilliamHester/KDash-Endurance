import React, { useEffect, useState } from "react";
import { Outlet } from "react-router";
import { Link, useNavigate } from "react-router-dom";
import { ConnectRequest, LiveTelemetryEvent } from "./live_telemetry_service_pb.js";
import { LiveTelemetryServiceClient } from "./live_telemetry_service_grpc_web_pb.js";
import "./base.css";

function App() {
  const [currentUser, setCurrentUser] = useState({});
  const navigate = useNavigate();

  useEffect(() => {
    const liveTelemetryServiceClient = new LiveTelemetryServiceClient('http://localhost:8000/api');
    const request = new ConnectRequest();
    const stream = liveTelemetryServiceClient.connect(request, {}, (err, resp) => {
        console.log("went here");
        console.log(err, resp);
    });
    stream.on('data', response => {
      console.log('response: ', response.getLapLog().getLapNum());
    });
    stream.on('status', status => {
      console.log(status);
    });
    stream.on('end', end => {
      console.log('Stream end');
    });
  }, []);

  return (
    <div className="column">
      <header className="header">
        <div className="wrapper site-header__wrapper">
          <Link className="no-style-link" to="/">
            <span className="website-name">Stintlytics</span>
          </Link>
        </div>

        <nav className="nav-right">
          <Link className="no-style-link" to="/upload">
            <span className="upload">+</span>
          </Link>
        </nav>
      </header>

      <Outlet />
    </div>
  );
};

export default App;