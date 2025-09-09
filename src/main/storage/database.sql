
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
  -- The driver car's completed race distance
  DriverDistance float NOT NULL,
  -- The telemetry data proto, serialized to bytes.
  Data bytea NOT NULL,

  PRIMARY KEY (SessionID, SubSessionID, SimSessionNumber, CarNumber, SessionTime)
);

CREATE INDEX TelemetryDataByDriverDistance
ON TelemetryData (SessionID, SubSessionID, SimSessionNumber, CarNumber, DriverDistance DESC);

CREATE OR REPLACE FUNCTION notify_on_telemetry_data()
RETURNS TRIGGER AS $$
DECLARE
  channel_name TEXT;
BEGIN
  channel_name := format('td_%s_%s_%s_%s', NEW.SessionID, NEW.SubSessionID, NEW.SimSessionNumber, NEW.CarNumber);
  PERFORM pg_notify(channel_name, NEW.SessionTime::TEXT);
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER telemetry_data_insert_trigger
AFTER INSERT ON TelemetryData
FOR EACH ROW
EXECUTE FUNCTION notify_on_telemetry_data();

CREATE TABLE DriverLaps (
  -- The SessionID variable from WeekendInfo in the session string
  SessionID int NOT NULL,
  -- The SubSessionID variable from WeekendInfo in the session string
  SubSessionID int NOT NULL,
  -- CurrentSessionNum from SessionInfo in the session string
  SimSessionNumber int NOT NULL,
  -- The number of the team
  CarNumber character varying (4) NOT NULL,
  -- The globally unique ID of the lap, incrementing every time a new lap is added to the database
  LapID SERIAL,
  -- The lap number
  LapNum int NOT NULL,
  -- The lap data
  LapEntry bytea NOT NULL,

  PRIMARY KEY (SessionID, SubSessionID, SimSessionNumber, CarNumber, LapNum)
);

CREATE OR REPLACE FUNCTION notify_on_driver_lap()
RETURNS TRIGGER AS $$
DECLARE
  channel_name TEXT;
BEGIN
  channel_name := format('dl_%s_%s_%s_%s', NEW.SessionID, NEW.SubSessionID, NEW.SimSessionNumber, NEW.CarNumber);
  PERFORM pg_notify(channel_name, NEW.LapID::TEXT);
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER driver_lap_insert_trigger
AFTER INSERT ON DriverLaps
FOR EACH ROW
EXECUTE FUNCTION notify_on_driver_lap();

CREATE TABLE DriverStints (
  -- The SessionID variable from WeekendInfo in the session string
  SessionID int NOT NULL,
  -- The SubSessionID variable from WeekendInfo in the session string
  SubSessionID int NOT NULL,
  -- CurrentSessionNum from SessionInfo in the session string
  SimSessionNumber int NOT NULL,
  -- The number of the team
  CarNumber character varying (4) NOT NULL,
  -- The globally unique ID of the stint, incrementing every time a new stint is added to the database
  StintID SERIAL,
  -- The in-lap number of the stint
  InLapNum int NOT NULL,
  -- The stint data
  StintEntry bytea NOT NULL,

  PRIMARY KEY (SessionID, SubSessionID, SimSessionNumber, CarNumber, InLapNum)
);

CREATE OR REPLACE FUNCTION notify_on_driver_stint()
RETURNS TRIGGER AS $$
DECLARE
  channel_name TEXT;
BEGIN
  channel_name := format('ds_%s_%s_%s_%s', NEW.SessionID, NEW.SubSessionID, NEW.SimSessionNumber, NEW.CarNumber);
  PERFORM pg_notify(channel_name, NEW.StintID::TEXT);
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER driver_stint_insert_trigger
AFTER INSERT ON DriverStints
FOR EACH ROW
EXECUTE FUNCTION notify_on_driver_stint();

CREATE TABLE OtherCarLaps (
  -- The SessionID variable from WeekendInfo in the session string
  SessionID int NOT NULL,
  -- The SubSessionID variable from WeekendInfo in the session string
  SubSessionID int NOT NULL,
  -- CurrentSessionNum from SessionInfo in the session string
  SimSessionNumber int NOT NULL,
  -- The number of the team
  CarNumber character varying (4) NOT NULL,
  -- The globally unique ID of the lap, incrementing every time a new lap is added to the database
  LapID SERIAL,
  -- The index of the other car
  OtherCarIdx int NOT NULL,
  -- The lap number
  LapNum int NOT NULL,
  -- The lap data
  LapEntry bytea NOT NULL,

  PRIMARY KEY (SessionID, SubSessionID, SimSessionNumber, CarNumber, OtherCarIdx, LapNum)
);

CREATE OR REPLACE FUNCTION notify_on_other_car_lap()
RETURNS TRIGGER AS $$
DECLARE
  channel_name TEXT;
BEGIN
  channel_name := format('ocl_%s_%s_%s_%s', NEW.SessionID, NEW.SubSessionID, NEW.SimSessionNumber, NEW.CarNumber);
  PERFORM pg_notify(channel_name, NEW.LapID::TEXT);
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER other_car_lap_insert_trigger
AFTER INSERT ON OtherCarLaps
FOR EACH ROW
EXECUTE FUNCTION notify_on_other_car_lap();
