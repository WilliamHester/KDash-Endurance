import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';

import { ListSessionsRequest } from '../live_telemetry_service_pb.js';
import { LiveTelemetryServiceClient } from '../live_telemetry_service_grpc_web_pb.js';
import { useNavigate } from 'react-router-dom';

/**
 * A React component that fetches and displays a list of iRacing sessions in a table.
 *
 * @param {object} props - The component props.
 * @param {function} props.setCurrentSession - Callback function to set the currently
 * selected session in the parent component.
 */
function SessionCarListPage() {
  const navigate = useNavigate();
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    try {
      const client = new LiveTelemetryServiceClient(`${location.origin}/api`);
      const request = new ListSessionsRequest();

      // 3. Make the RPC call.
      client.listSessions(request, {}, (err, response) => {
        if (err) {
          console.error(`gRPC Error: ${err.message}`);
          setError(err);
        } else {
          // The generated code provides getter methods for the fields.
          setSessions(response.getSessionsList());
        }
        setLoading(false);
      });
    } catch (e) {
      console.error("Failed to initiate gRPC call:", e);
      setError(e);
      setLoading(false);
    }
  }, []); // The empty dependency array ensures this effect runs only once.

  const handleRowClick = (session) => {
    console.log('Selected Session:', session.toObject());
    navigate(`/${session.getSessionId()}/${session.getSubSessionId()}/${session.getSimSessionNumber()}/${session.getCarNumber()}`);
  };

  if (loading) {
    return <p>Loading sessions...</p>;
  }

  if (error) {
    return <p><strong>Error:</strong> Failed to load sessions. ({error.message})</p>;
  }

  return (
    <div style={{ fontFamily: 'sans-serif' }}>
      <h2>🏁 Session List</h2>
      <p>Click on a row to select a session.</p>
      <table style={styles.table}>
        <thead>
          <tr>
            <th style={styles.th}>Session ID</th>
            <th style={styles.th}>Sub Session ID</th>
            <th style={styles.th}>Sim Session #</th>
            <th style={styles.th}>Car #</th>
            <th style={styles.th}>Track</th>
          </tr>
        </thead>
        <tbody>
          {sessions.length > 0 ? (
            sessions.map((session) => (
              <tr
                key={`${session.getSessionId()}-${session.getSubSessionId()}`}
                onClick={() => handleRowClick(session)}
                className="session-row"
              >
                <td style={styles.td}>{session.getSessionId()}</td>
                <td style={styles.td}>{session.getSubSessionId()}</td>
                <td style={styles.td}>{session.getSimSessionNumber()}</td>
                <td style={styles.td}>{session.getCarNumber()}</td>
                <td style={styles.td}>{session.getTrackName()}</td>
              </tr>
            ))
          ) : (
            <tr>
              <td colSpan="5" style={styles.td}>No sessions found.</td>
            </tr>
          )}
        </tbody>
      </table>
      {/* Basic hover effect using a style tag */}
      <style>{`
        .session-row {
          cursor: pointer;
        }
        .session-row:hover {
          background-color: #f0f0f0;
        }
      `}</style>
    </div>
  );
}

// PropTypes for type-checking the component's props
SessionCarListPage.propTypes = {
  setCurrentSession: PropTypes.func.isRequired,
};

// Basic inline styles for the table
const styles = {
  table: {
    width: '100%',
    borderCollapse: 'collapse',
    boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
  },
  th: {
    borderBottom: '2px solid #ddd',
    padding: '12px',
    textAlign: 'left',
    backgroundColor: '#fafafa',
    fontWeight: '600',
  },
  td: {
    borderBottom: '1px solid #ddd',
    padding: '12px',
  },
};

export default SessionCarListPage;
