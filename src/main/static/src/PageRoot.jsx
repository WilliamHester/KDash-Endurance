import React from 'react';
import SessionCarListPage from './sessioncarlist/SessionCarListPage';
import SessionPage from './SessionPage';
import { BrowserRouter, Routes, Route } from 'react-router-dom';

function PageRoot() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<SessionCarListPage />} />
        <Route path="/:sessionId/:subSessionId/:simSessionNumber/:carNumber/*" element={<SessionPage/>} />
      </Routes>
    </BrowserRouter>
  );
}

export default PageRoot;
