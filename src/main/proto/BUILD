load("@rules_java//java:defs.bzl", "java_proto_library")
load("@rules_proto//proto:defs.bzl", "proto_library")
load("@io_grpc_grpc_java//:java_grpc_library.bzl", "java_grpc_library")

java_proto_library(
    name = "live_telemetry_service_java_proto",
    deps = [":live_telemetry_service"],
    visibility = ["//visibility:public"],
)

java_grpc_library(
    name = "live_telemetry_service_java_grpc",
    srcs = [":live_telemetry_service"],
    deps = ["live_telemetry_service_java_proto"],
    visibility = ["//visibility:public"],
)

proto_library(
    name = "live_telemetry_service",
    srcs = ["live_telemetry_service.proto"],
    deps = [
        "@com_google_protobuf//:timestamp_proto",
    ]
)