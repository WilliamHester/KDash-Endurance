load("@rules_java//toolchains:toolchain_utils.bzl", "find_java_runtime_toolchain")

def launch4j_exe(name, config_template, jar, **kwargs):
    """Generates a launch4j .exe from a jar.

    Args:
        name: The name of the output rule (will create name.exe).
        config_template: The .xml.template file.
                         It must contain __JAR_PATH__ and __OUT_FILE__ placeholders.
        jar: The jar target to wrap.
        **kwargs: Standard rule arguments (visibility, tags, etc.).
    """

    out_exe = name + ".exe"
    generated_config = name + "_config.xml"

    native.genrule(
        name = name + "_config_generator",
        srcs = [
            config_template,
            jar,
        ],
        outs = [generated_config],
        # Note: we have to use "realpath" here, because the location of the .jar in launch4j is either relative to the
        # config file's path or absolute.
        cmd = """
            sed \
                -e "s|__JAR_PATH__|$$(realpath $(location {jar}))|" \
                -e 's|__OUT_FILE__|{out_exe}|' \
                $(location {config_template}) > $@
        """.format(
            jar = jar,
            out_exe = out_exe,
            config_template = config_template,
        ),
        visibility = ["//visibility:private"],
    )

    native.genrule(
        name = name,
        srcs = [
            ":" + generated_config,
            jar,
            "@launch4j//:launch4j_deploy.jar",
            "@launch4j//:bin",
            "@launch4j//:w32api",
            "@launch4j//:head",
        ],
        outs = [out_exe],
        cmd = """
            mkdir tool_dir
            cp $(location @launch4j//:launch4j_deploy.jar) tool_dir/launch4j_deploy.jar

            # Copy all the dependencies to the directories that are expected by launch4j
            mkdir tool_dir/bin
            cp $(locations @launch4j//:bin) tool_dir/bin/
            mkdir tool_dir/w32api
            cp $(locations @launch4j//:w32api) tool_dir/w32api/
            mkdir tool_dir/head
            cp $(locations @launch4j//:head) tool_dir/head/

            java -jar tool_dir/launch4j_deploy.jar $(location :{generated_config})
        """.format(
            generated_config = generated_config,
        ),
        **kwargs,
    )