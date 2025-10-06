load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "store",
    srcs = glob(["*.kt"]),
    data = [":db.password"],
    visibility = ["//main/java/me/williamhester/kdash/web:__subpackages__"],
    deps = [
        "@maven//:com_impossibl_pgjdbc_ng_pgjdbc_ng",
        "@maven//:com_google_flogger_flogger",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_protobuf_protobuf_java_util",
        "@kdash//main/java/me/williamhester/kdash/api",
        "//main/java/me/williamhester/kdash/web/extensions",
        "//main/java/me/williamhester/kdash/web/models",
        "//main/proto:live_telemetry_service_kt_proto",
        "//main/proto:storage_java_proto",
    ],
)
