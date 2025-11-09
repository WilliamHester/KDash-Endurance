load("@rules_java//java:defs.bzl", "java_binary")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "client",
    srcs = glob(["*.kt"]),
    data = [":localhost.crt"],
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
        "//main/java/me/williamhester/kdash/web/common",
        "//main/proto:live_telemetry_pusher_service_kt_grpc",
        "//main/proto:live_telemetry_pusher_service_kt_proto",
    ],
)

java_binary(
    name = "local_client",
    main_class = "me.williamhester.kdash.web.client.LocalClient",
    runtime_deps = [":client"],
    visibility = ["//visibility:public"],
)

java_binary(
    name = "prod_client",
    main_class = "me.williamhester.kdash.web.client.ProdClient",
    runtime_deps = [":client"],
    visibility = ["//visibility:public"],
)
