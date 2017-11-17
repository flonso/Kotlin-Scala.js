package ch.epfl.k2sjsir;

import org.jetbrains.kotlin.cli.common.arguments.Argument;
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments;
import org.jetbrains.kotlin.cli.common.arguments.DefaultValues;
import org.jetbrains.kotlin.cli.common.arguments.GradleOption;

import static org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.MODULE_PLAIN;

public class K2SJSIRCompilerArgs extends CommonCompilerArguments {

    @GradleOption(DefaultValues.StringNullDefault.class)
    @Argument(value = "-d", description = "Destination for generated class files", valueDescription = "<directory|jar>")
    public String destination = null;

    @Argument(value = "classpath", description = "Paths where to find user class files", valueDescription = "<path>")
    public String classpath = null;

    @GradleOption(DefaultValues.BooleanTrueDefault.class)
    @Argument(value = "-meta-info", description = "Generate .meta.js and .kjsm files with metadata. Use to create a library")
    public Boolean metaInfo = null;

    @GradleOption(DefaultValues.StringNullDefault.class)
    @Argument(value = "-output", description = "Output file path", valueDescription = "<path>")
    public String outputFile = null;

    @GradleOption(DefaultValues.BooleanFalseDefault.class)
    @Argument(value = "-source-map", description = "Generate source map")
    public Boolean sourceMap = null;

    @GradleOption(DefaultValues.JsModuleKinds.class)
    @Argument(
            value = "-module-kind",
            description = "Kind of a module generated by compiler",
            valueDescription = "{ plain, amd, commonjs, umd }"
    )
    public String moduleKind = MODULE_PLAIN;

    @GradleOption(DefaultValues.JsEcmaVersions.class)
    @Argument(value = "target", description = "Generate JS files for specific ECMA version", valueDescription = "{ v5 }")
    public String target = null;

    @GradleOption(DefaultValues.BooleanTrueDefault.class)
    @Argument(value = "no-stdlib", description = "Don't include Kotlin runtime into classpath")
    public Boolean noStdlib = false;

    @Argument(value = "libraries", description = "Paths to Kotlin libraries with .meta.js and .kjsm files, separated by system file separator", valueDescription = "<path>")
    public String libraries = null;

    @GradleOption(DefaultValues.JsMain.class)
    @Argument(value = "main", description = "Whether a main function should be called", valueDescription =  "{call, noCall}")
    public String main = null;

    // Paths to output directories for friend modules.
    public String[] friendPaths = null;
}
