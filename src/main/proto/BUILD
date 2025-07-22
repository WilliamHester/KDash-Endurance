load("@rules_java//java:defs.bzl", "java_proto_library")
load("@rules_proto//proto:defs.bzl", "proto_library")
load("@grpc-java//:java_grpc_library.bzl", "java_grpc_library")
load("@grpc-kotlin//:kt_jvm_grpc.bzl", "kt_jvm_grpc_library")
load("@grpc-kotlin//:kt_jvm_grpc.bzl", "kt_jvm_proto_library")

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
        "@protobuf//:timestamp_proto",
    ]
)

kt_jvm_proto_library(
    name = "live_telemetry_service_kt_proto",
    deps = [":live_telemetry_service"],
    visibility = ["//visibility:public"],
)

java_proto_library(
    name = "live_telemetry_pusher_service_java_proto",
    deps = [":live_telemetry_pusher_service"],
    visibility = ["//visibility:public"],
)

kt_jvm_proto_library(
    name = "live_telemetry_pusher_service_kt_proto",
    deps = [":live_telemetry_pusher_service"],
    visibility = ["//visibility:public"],
)

kt_jvm_grpc_library(
    name = "live_telemetry_pusher_service_kt_grpc",
    srcs = [":live_telemetry_pusher_service"],
    deps = ["live_telemetry_pusher_service_java_proto"],
    visibility = ["//visibility:public"],
)

proto_library(
    name = "live_telemetry_pusher_service",
    srcs = ["live_telemetry_pusher_service.proto"],
    deps = [
        "@protobuf//:any_proto",
        "@protobuf//:descriptor_proto",
    ]
)