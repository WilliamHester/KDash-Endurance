load("@rules_pkg//pkg:deb.bzl", "pkg_deb")
load(":launch4j.bzl", "launch4j_exe")

pkg_deb(
    name = "web-server-deb",
    architecture = "amd64",
    package = "kdash-web-server",
    version = "1.0.0",
    maintainer = "William Hester",
    description = "The web server for Stintlytics.",
    data = "//main/java/me/williamhester/kdash/web:web_main_deploy.jar",
)

pkg_deb(
    name = "data-logger-deb",
    architecture = "amd64",
    package = "kdash-data-logger-server",
    version = "1.0.0",
    maintainer = "William Hester",
    description = "The server for logging data to the database.",
    data = "//main/java/me/williamhester/kdash/web:web_main_deploy.jar",
)

launch4j_exe(
    name = "client",
    config_template = ":client_launch4j_config.xml.template",
    jar = "//main/java/me/williamhester/kdash/web/client:prod_client_deploy.jar",
)
