
CREATE TABLE TelemetryData (
  -- The SessionID variable from WeekendInfo in the session string
  SessionID int NOT NULL,
  -- The SubSessionID variable from WeekendInfo in the session string
  SubSessionID int NOT NULL,
  -- CurrentSessionNum from SessionInfo in the session string
  SimSessionNumber int NOT NULL,
  -- The number of the team
  CarNumber character varying (4) NOT NULL,
  -- The time in the session the data is logged from
  SessionTime double precision NOT NULL,
  -- The telemetry data proto, serialized to bytes.
  Data bytea NOT NULL,

  PRIMARY KEY (SessionID, SubSessionID, SimSessionNumber, CarNumber, SessionTime)
);

CREATE TABLE SessionCars (
  -- The SessionID variable from WeekendInfo in the session string
  SessionID int NOT NULL,
  -- The SubSessionID variable from WeekendInfo in the session string
  SubSessionID int NOT NULL,
  -- CurrentSessionNum from SessionInfo in the session string
  SimSessionNumber int NOT NULL,
  -- The number of the team
  CarNumber character varying (3) NOT NULL,
  -- The current Session metadata proto, serialized to bytes.
  Metadata bytea NOT NULL,

  PRIMARY KEY (SessionID, SubSessionID, SimSessionNumber, CarNumber)
);

CREATE TABLE DriverLaps (
  -- The SessionID variable from WeekendInfo in the session string
  SessionID int NOT NULL,
  -- The SubSessionID variable from WeekendInfo in the session string
  SubSessionID int NOT NULL,
  -- CurrentSessionNum from SessionInfo in the session string
  SimSessionNumber int NOT NULL,
  -- The number of the team
  CarNumber character varying (4) NOT NULL,
  -- The lap number
  LapNum int NOT NULL,
  -- The lap data
  LapData bytea NOT NULL,

  PRIMARY KEY (SessionID, SubSessionID, SimSessionNumber, CarNumber, LapNum)
);
