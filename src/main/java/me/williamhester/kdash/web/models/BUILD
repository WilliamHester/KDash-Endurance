load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "models",
    srcs = glob(["*.kt"]),
    visibility = [
        "//main/java/me/williamhester/kdash/web:__subpackages__",
        "//main/javatests/me/williamhester/kdash/web:__subpackages__",
    ],
    deps = [
        "//main/proto:live_telemetry_service_java_grpc",
        "//main/proto:live_telemetry_pusher_service_java_proto",
        "//main/proto:storage_java_proto",
    ],
)
