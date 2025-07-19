load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "client",
    srcs = glob(["*.kt"]),
    runtime_deps = [
        "@maven//:com_google_flogger_flogger_system_backend",
        "@maven//:org_slf4j_slf4j_simple",
    ],
    deps = [
        "@maven//:com_google_flogger_flogger",
        "@maven//:io_grpc_grpc_api",
        "@maven//:io_grpc_grpc_netty",
        "@maven//:io_grpc_grpc_netty_shaded",
        "@maven//:io_grpc_grpc_protobuf",
        "@maven//:io_grpc_grpc_stub",
        "@maven//:com_google_protobuf_protobuf_java_util",
        "@maven//:org_mindrot_jbcrypt",
        "@kdash//main/java/me/williamhester/kdash/api",
        "@kdash//main/java/me/williamhester/kdash/monitors",
        "//main/proto:live_telemetry_pusher_service_kt_grpc",
        "//main/proto:live_telemetry_pusher_service_kt_proto",
    ],
)

java_binary(
    name = "client_main",
    main_class = "me.williamhester.kdash.web.client.Client",
    runtime_deps = [":client"],
)

#java_binary(
#    name = "desktop_client_main",
#    main_class = "me.williamhester.kdash.web.DesktopMainKt",
#    runtime_deps = [":web"],
#)
