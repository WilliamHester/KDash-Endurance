load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "monitors",
    srcs = glob(["*.kt"]),
    visibility = ["//main/java/me/williamhester/kdash/web:__pkg__"],
    deps = [
        "@maven//:com_google_flogger_flogger",
        "//main/proto:live_telemetry_pusher_service_kt_proto",
        "//main/proto:live_telemetry_service_kt_proto",
        "//main/java/me/williamhester/kdash/web/extensions",
        "//main/java/me/williamhester/kdash/web/state",
    ],
)
