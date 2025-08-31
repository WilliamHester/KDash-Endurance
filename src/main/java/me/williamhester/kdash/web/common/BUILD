load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "common",
    srcs = glob(["*.kt"]),
    deps = ["@maven//:io_grpc_grpc_stub"],
    visibility = ["//visibility:public"],
)
