load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "web",
    srcs = glob(["*.kt"]),
    runtime_deps = [
        "@maven//:com_google_flogger_flogger_system_backend",
        "@maven//:org_slf4j_slf4j_simple",
    ],
    visibility = ["//main/javatests/me/williamhester/kdash/web:__pkg__"],
    deps = [
        "@maven//:com_google_flogger_flogger",
        "@maven//:io_grpc_grpc_api",
        "@maven//:io_grpc_grpc_netty_shaded",
        "@maven//:io_grpc_grpc_protobuf",
        "@maven//:io_grpc_grpc_stub",
        "@maven//:io_javalin_javalin",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_protobuf_protobuf_java_util",
        "@maven//:org_mindrot_jbcrypt",
        "@kdash//main/java/me/williamhester/kdash/api",
        "@kdash//main/java/me/williamhester/kdash/monitors",
        "//main/proto:live_telemetry_service_java_grpc",
    ],
)

java_binary(
    name = "web_main",
    main_class = "me.williamhester.kdash.web.MainKt",
    runtime_deps = [":web"],
)

java_binary(
    name = "desktop_web_main",
    main_class = "me.williamhester.kdash.web.DesktopMainKt",
    runtime_deps = [":web"],
)
