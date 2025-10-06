import React, { useState } from 'react';
import SessionCarListPage from './sessioncarlist/SessionCarListPage';
import SessionPage from './SessionPage';

function PageRoot() {
  const [currentSession, setCurrentSession] = useState(null);

  return (
    <div>
      {currentSession ? (
        <SessionPage session={currentSession} />
      ) : (
        <SessionCarListPage setCurrentSession={setCurrentSession} />
      )}
    </div>
  );
}

export default PageRoot;
