package(default_visibility = ["//visibility:public"])

java_binary(
    name = "importscrubber",
    srcs = [
        "ImportScrubber.java",
        "ScrubTask.java",
        "SourceFile.java",
        "StatementFormat.java",
        "ImportStatement.java",
        "ImportStatementComparator.java",
        "ImportStatements.java",
        "JavaFileFilter.java",
        "PackageStmt.java",
        "ClassParserWrapper.java",
        "PrintListener.java",
        "FilePair.java",
        "IProgressMonitor.java",
        "IReferenceFoundListener.java",
        "FileChooser.java",
        "Resources.java",
    ],
    main_class = "org.pubref.util.importscrubber.ImportScrubber",
    deps = [
        "@org_apache_bcel_bcel//jar",
    ]
)
