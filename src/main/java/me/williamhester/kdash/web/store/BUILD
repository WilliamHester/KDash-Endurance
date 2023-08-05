load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "store",
    srcs = glob(["*.kt"]),
    data = [":db.password"],
    visibility = ["//main/java/me/williamhester/kdash/web:__subpackages__"],
    runtime_deps = [
        "@maven//:mysql_mysql_connector_java",
    ],
    deps = [
        "@maven//:com_google_flogger_flogger",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_protobuf_protobuf_java_util",
        "@kdash//main/java/me/williamhester/kdash/api",
        "//main/java/me/williamhester/kdash/web/models",
    ],
)