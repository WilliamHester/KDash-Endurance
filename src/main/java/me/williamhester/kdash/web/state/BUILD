load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "state",
    srcs = glob(["*.kt"]),
    visibility = ["//main/java/me/williamhester/kdash/web:__subpackages__"],
    deps = [
        "@maven//:com_google_flogger_flogger",
        "//main/proto:live_telemetry_pusher_service_kt_proto",
    ],
)
