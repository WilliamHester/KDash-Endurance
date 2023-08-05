load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "session",
    srcs = glob(["*.kt"]),
    visibility = ["//main/java/me/williamhester/kdash/web:__subpackages__"],
    deps = [
        "//main/java/me/williamhester/kdash/web/store",
    ]
)