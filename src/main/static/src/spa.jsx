import React from 'react';
// import ReactDOM from 'react-dom';
import ReactDOM from "react-dom/client";
import {
  BrowserRouter,
  Routes,
  Route,
} from "react-router-dom";
import LapLogPage from "./laplog/LapLogPage";
import GapsPage from "./gaps/GapsPage";
import App from "./App.jsx";

const root = ReactDOM.createRoot(
  document.getElementById("root")
);
root.render(
  <BrowserRouter>
    <Routes>
      <Route path="/" element={<App />}>
        <Route path="" element={<LapLogPage />} />
        <Route path="laps" element={<LapLogPage />} />
        <Route path="gaps" element={<GapsPage />} />
      </Route>
    </Routes>
  </BrowserRouter>
);
