import React from 'react';
import SessionCarListPage from './sessioncarlist/SessionCarListPage';
import SessionPage from './SessionPage';
import { BrowserRouter, Routes, Route } from 'react-router-dom';

function PageRoot() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<SessionCarListPage />} />
        {/* TODO: It looks like this is getting pre-loaded, but that's causing it to fail to load because the null info for the session ID isn't there. */}
        <Route path="/:sessionId/:subSessionId/:simSessionNumber/:carNumber/*" element={<SessionPage/>} />
      </Routes>
    </BrowserRouter>
  );
}

export default PageRoot;
