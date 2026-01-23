load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library", "kt_jvm_test")

kt_jvm_test(
    name = "query_test",
    srcs = ["QueryTest.kt"],
    test_class = "me.williamhester.kdash.web.service.telemetry.query.QueryTest",
    associates = [
        "//main/java/me/williamhester/kdash/web/service/telemetry/query",
    ],
    deps = [
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)

kt_jvm_test(
    name = "processors_test",
    srcs = ["ProcessorsTest.kt"],
    test_class = "me.williamhester.kdash.web.service.telemetry.query.ProcessorsTest",
    associates = [
        "//main/java/me/williamhester/kdash/web/service/telemetry/query",
    ],
    deps = [
        "//main/proto:live_telemetry_service_java_grpc",
        "//main/proto:live_telemetry_pusher_service_kt_grpc",
        "//main/proto:live_telemetry_pusher_service_kt_proto",
        "//main/proto:storage_kt_proto",
        "//main/java/me/williamhester/kdash/web/models",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)