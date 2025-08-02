load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "query",
    srcs = glob(["*.kt"]),
    visibility = [
        "//main/java/me/williamhester/kdash/web:__subpackages__",
        "//main/javatests/me/williamhester/kdash/web:__subpackages__",
    ],
    deps = [
        "@maven//:io_grpc_grpc_protobuf",
        "//main/proto:live_telemetry_pusher_service_kt_proto",
        "//main/java/me/williamhester/kdash/web/models",
    ],
)
