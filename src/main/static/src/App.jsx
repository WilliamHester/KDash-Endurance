import React, { useEffect, useState } from "react";
import { Outlet } from "react-router";
import { Link, NavLink } from "react-router-dom";
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
          <NavLink className="no-style-link hamburgerMenuItem" to="/fuelcharts">Fuel Charts</NavLink>
          <NavLink className="no-style-link hamburgerMenuItem" to="/trackmap">Track Map</NavLink>
        </div>

        <div className="column content">
          <Outlet />
        </div>
      </div>
    </div>
  );
};

export default App;