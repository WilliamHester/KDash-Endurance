#!/bin/sh

DIR=../proto/
OUT_DIR=./src/lib/grpc/

protoc \
  --plugin=./node_modules/.bin/protoc-gen-ts_proto \
  --ts_proto_out=$OUT_DIR \
  --ts_proto_opt=esModuleInterop=true \
  --ts_proto_opt=outputServices=nice-grpc \
  --ts_proto_opt=outputServices=generic-definitions \
  --ts_proto_opt=useExactTypes=false \
  -I=$DIR \
  live_telemetry_service.proto


#  --ts_proto_opt=outputClientImpl=grpc-web \
