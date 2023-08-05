#!/bin/sh

DIR=../proto/
OUT_DIR=./src

protoc \
  -I=$DIR \
  live_telemetry_service.proto \
  --js_out=import_style=commonjs:$OUT_DIR

protoc \
  -I=$DIR \
  live_telemetry_service.proto \
  --grpc-web_out=import_style=commonjs,mode=grpcwebtext:$OUT_DIR
