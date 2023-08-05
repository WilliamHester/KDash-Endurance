CREATE DATABASE IF NOT EXISTS kdash;

USE kdash;

CREATE TABLE IF NOT EXISTS Users (
  UserId int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  Username varchar(255) NOT NULL,
  Email varchar(255) NOT NULL,
  Password varchar(255) DEFAULT NULL,
  FirstName varchar(32) NOT NULL,
  LastName varchar(32) NOT NULL
) DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS Sessions (
  SessionId varchar(255) PRIMARY KEY,
  UserId int NOT NULL,
  TrackId int,
  TrackName varchar(255),
  CarId int,
  SessionDate TIMESTAMP,
  FastestLap float,
  NumLaps int
) DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS UserSessions (
  SessionId varchar(255) PRIMARY KEY,
  UserId int NOT NULL
) DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS UserTargetLaps (
  UserId INT NOT NULL,
  TrackId INT NOT NULL,
  CarId INT NOT NULL,
  LapSessionId VARCHAR(255) NOT NULL,
  LapNum INT NOT NULL,
  PRIMARY KEY (UserId, TrackId, CarId)
) DEFAULT CHARSET=utf8;
