# Bazel Importscrubber

A fork of `net.sourceforge.importscrubber` used to demonstrate the use
of [Bazel][bazel-home] for java developers.

## About Bazel

[Bazel][bazel-home] is Google's open-source version of their internal
tool `blaze` used to build the majority of Google's software products.

## About Importscrubber

Importscrubber is a java program that parses java files and cleans up
the import statements.  The program is ancient, dating back to the
late 1990s, and still available on sourceforge.  These days you can do
import statement cleanup with Eclipse (and probably many other IDEs),
but for developers not using a dedicated java IDE using an external
tool to convert those pesky `java.util.*` statements into
`java.util.Map` can still be useful.

I thought adapting importscrubber for bazel would be a good starter
project for demonstration of bazel's java support as it is fairly
simple but not excessively trivial.  We can also use it to demonstrate
how to integrate it with Bazel itself via Bazel's extension mechanism
(Skylark).


## Getting Started with Bazel

Download bazel and [install][bazel-install] on your system.  I used
the `bazel-0.2.3-jdk7-installer-darwin-x86_64.sh` script that installs
the `bazel` command to `~/bin` path.  You'll want to have this on your
`PATH`.

```sh
$ chmod +x bazel-0.2.3-jdk7-installer-darwin-x86_64.sh

# Install bazel to ~/bin
$ ./bazel-0.2.3-jdk7-installer-darwin-x86_64.sh

# Run the 'version' command
$ ~/bin/bazel version
Build label: 0.2.3-jdk7
Build target: bazel-out/local-fastbuild/bin/src/main/java/com/google/devtools/build/lib/bazel/BazelServer_deploy.jar
Build time: Tue May 17 14:25:11 2016 (1463495111)
Build timestamp: 1463495111
Build timestamp as int: 1463495111
```

The shell script at `~/bin/bazel` invokes the "real bazel" script in
`~/.bazel/bin/bazel-real`.  This is a compiled program that
ulitimately runs as a long-lived java server process (one per
WORKSPACE) that shuts itself down after 3 hours of inactivity.  Here's
what this process looks like on my system:

```sh
$ ps -ef | grep bazel
501  5003     1   0  7:07AM ??         0:51.08 bazel(github) -server
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/private/var/tmp/_bazel_pcj/d1b2db2f41e7ad5dbe0e625d9117d17e
-Xverify:none
-Djava.util.logging.config.file=/private/var/tmp/_bazel_pcj/d1b2db2f41e7ad5dbe0e625d9117d17e/javalog.properties
-Djava.library.path=/var/tmp/_bazel_pcj/install/abd9ee28d8a31141dd2953459e151fbb/_embedded_binaries/embedded_tools/tools/objc:/var/tmp/_bazel_pcj/install/abd9ee28d8a31141dd2953459e151fbb/_embedded_binaries/
-Dfile.encoding=ISO-8859-1
-jar /var/tmp/_bazel_pcj/install/abd9ee28d8a31141dd2953459e151fbb/_embedded_binaries/A-server.jar
--max_idle_secs 10800
--install_base=/var/tmp/_bazel_pcj/install/abd9ee28d8a31141dd2953459e151fbb
--install_md5=abd9ee28d8a31141dd2953459e151fbb
--output_base=/private/var/tmp/_bazel_pcj/d1b2db2f41e7ad5dbe0e625d9117d17e
--workspace_directory=/Users/pcj/github/bazel-importscrubber
--nodeep_execroot
--nofatal_event_bus_exceptions
--option_sources=
```

## Repository Layout

(This section is not specific to bazel).

This code in this repo was copied from [importscour][importscour-home]
as this was on GitHub and easily cloneable.

I prepared a new git repo with `git init` and copied over the java
source files into `src/main/java`.  I've renamed the java package from
`net.sourceforge.importscrubber` to `org.pubref.util.importscrubber`
for two reasons:

1. I associate crappy software with sourceforge.  I've therefore
   changed the branding (sorry sourceforge).

1. It implies that `pubref` will be maintaining this fork.  Pull
   requests welcome.

In preparing a new repo I've also adopted a standard maven directory
layout with `src/main/java` and `src/test/java`.  Bazel plays nice
with this directory layout.


## Relevant Bazel Commands

You can get more information about bazel command via the `bazel help`
command.  In this post we'll be using three bazel commands:

1. `bazel build`: compile and build program code.
1. `bazel test`: run our tests.
1. `bazel run`: execute our final executable jar.


## Relevant Bazel Configuration Files

* **`WORKSPACE`**: a file that defines the project root.  Python-like
rule syntax.  Required, but can be empty.

* **`BUILD`**: a file that defines a project package.  Python-like
  syntax rule.  Best practice is to create a BUILD file for every
  subdirectory in your project that has stuff to build.  Required to
  do anything useful.

* **`tools/bazel.rc`**: project-level bazel resource file.  Used to
  configure options for commands for the entire project (to be checked
  into version control to for shared bazel configuration for all
  project developers). Optional.

* **~/.bazelrc**: user-level bazel resource file.  Use to configure
  options for you.  Optional.

* **/etc/bazel.bazelrc**: system-level bazel resource file.  Use to
  configure options for all users on the system.  Optional.

The python-like syntax is called `Skylark` (in the context of writing
bazel extensions).


## Relevant Bazel Nomenclature

In general when you invoke the *bazel client* `~/bin/bazel` you
specify a **bazel command** and a **target-pattern** that identifies a
set of targets to compute.  Bazel *loads* all the resources needed to
compute a dependency graph for those targets, *analyzes* the
dependency graph to figure out what **rules** to run, and the
*executes* those rules.  Bazel is pretty smart about caching the
outputs of nodes within that dependency graph to do work as
efficienctly as possible in an incremental and parallel fashion.

* **bazel command**: The name of the command to invoke.  Each command
  takes a number of different options.  Examples: `build`, `test`,
  `run`.

* **target pattern**: A heirarchical path selection syntax that
  identifies nodes in dependency graph.  Special operators include `/`
  (slash character, for path traversal relative to a folder), `//`
  (double-slash, for path traversal from the project root), `:` (colon
  character, for path traversal within a `BUILD` file), and `@`
  (at-sign character, for path traversal relative to an external
  dependency named in the WORKSPACE).  Examples:
  `//src/main/java:src_files`, `:importscrubber_bin`,
  `@apache_commons_bcel_bcel//jar`.

* **rule**: A python-like function that performs some unit of work
  within bazel.  You can invoke rules that are built-in to bazel
  itself, or load new rules into your project from the WORKSPACE.
  Examples: `java_library` (build a jar file), `java_binary`
  (build/run an executable jar), `java_test` (run a test).


## Step 1: Create a `WORKSPACE` file and project layout.

The master per-repository project file is called `WORKSPACE`.  This
tells bazel where the root of your project is.  Later we'll define an
external depenency to a maven-hosted jar in this file.

```sh
$ cd bazel-importscrubber
$ git init
$ touch WORKSPACE
$ git add WORKSPACE
$ mkdir -p src/main/java src/test/java
```

If you like, read more about [rules that can go in the WORKSPACE file][bazel-workspace-rules].


## Step 2: Create a `BUILD` file where our java sources live.

For this project, all the source files exist within a single
directory.  In this example I'll be explicitly naming all the required
source files.  You can also use `glob` patterns if you like.

```sh
$ touch src/main/java/org/pubref/util/importscrubber/BUILD
```

Here's the `java_binary` rule that we'll use to build the code:

```python
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
```

I now have defined a node that can be referred to in the same BUILD
file as `:importscrubber` or anywhere within the project as
`//src/main/java/org/pubref/util/importscrubber:importscrubber`.  One can
setup aliases if desired.

The `deps` field is a list that has a single entry naming the single
external dependency for this project
([BCEL: Byte Code Engineering Library][bcel-home]).  This entry has a
funky syntax that reads "there is an external dependency named
`org_apache_bcel_bcel` in this WORKSPACE, and we want the jar node
within it".  In the next section of this tutorial we'll go back to our
workspace file and define that.

We can now invoke the `importscrubber` rule and build the library:

```sh
$ ~/bin/bazel build //src/main/java/org/pubref/util/importscrubber:importscrubber
INFO: Found 1 target...
Target //src/main/java/org/pubref/util/importscrubber:importscrubber up-to-date:
  bazel-bin/src/main/java/org/pubref/util/importscrubber/importscrubber.jar
  bazel-bin/src/main/java/org/pubref/util/importscrubber/importscrubber
INFO: Elapsed time: 0.101s, Critical Path: 0.00s
```

```sh
# Equivalent Alternative 1: shorthand syntax when the BUILD file at a particular
# location matches the name of the folder it's defined within.
$ ~/bin/bazel build //src/main/java/org/pubref/util/importscrubber

# Equivalent Alternative 2: if you're already in the same directory as the BUILD file.
$ (cd src/main/java/org/pubref/util/importscrubber && ~/bin/bazel build importscrubber)
```

**Gotcha**: If you inspect the contents of the jar file, you'll notice
that the BCEL dependency files are *not included*.  For jars that will
be run by the `bazel run` command, it won't matter.  However, to build
a fully self-contained executable jar that contains all dependencies
that you can deploy on any machine, *invoke the build rule with
`_deploy.jar` appended to the end*.  This is called an *implicit
output target*.


```sh
# Build a self-contained executable jar
$ ~/bin/bazel build //src/main/java/org/pubref/util/importscrubber:importscrubber_deploy.jar
```

## Running the code

We can run the executable jar either by invoking the executable jar
itself, or using the `bazel-run` command. Let's see how command line arguments work:

```sh
# Via the bazel-run command
$ java -jar bazel-bin/src/main/java/org/pubref/util/importscrubber/importscrubber_deploy.jar
Usage: importscrubber <CLASSES_DIR> <SOURCE_DIR> [Filename...]
```

However, since this particular program uses BCEL to inspect the
`.class` file foreach `.java` source it evaluates, it needs access to
a directory (CLASSES_DIR) where the class files can be found.  As it
turns out, for bazel this directory is
`bazel-out/local-fastbuild/bin/src/main/java/org/pubref/util/importscrubber/_javac/importscrubber/importscrubber_classes/`
or
`bazel-out/local-fastbuild/bin/${package_dirname}/_javac/${rule_name}/${rule_name}_classes/`

So let's run importscrubber on itself!

```sh
$ PACKAGE_PATH=org/pubref/util/importscrubber
$ JAR_FILE=bazel-bin/src/main/java/$PACKAGE_PATH/importscrubber_deploy.jar
$ CLASSES_DIR=bazel-out/local-fastbuild/bin/src/main/java/$PACKAGE_PATH/_javac/importscrubber/importscrubber_classes/$PACKAGE_PATH
$ SOURCE_DIR=src/main/java/$PACKAGE_PATH
$ java -jar $JAR_FILE $CLASSES_DIR $SOURCE_DIR ImportScrubber.java
importscrubber: task complete ImportScrubber.java
importscrubber: Done.
```

It works!  We can also use the `bazel-run` command:

```sh
$ JAR_FILE=bazel-bin/src/main/java/$PACKAGE_PATH/importscrubber_deploy.jar
$ CLASSES_DIR=bazel-out/local-fastbuild/bin/src/main/java/$PACKAGE_PATH/_javac/importscrubber/importscrubber_classes/$PACKAGE_PATH
$ SOURCE_DIR=src/main/java/$PACKAGE_PATH
$ ~/bin/bazel run //src/main/java/org/pubref/util/importscrubber $CLASSES_DIR $SOURCE_DIR ImportScrubber.java
// FAILS: can't find source directory.
```

## Adding a Maven Dependency in the `WORKSPACE`

Use the `maven_jar` in your project `WORKSPACE` file to define an
external dependency from a maven repository.  Here's what that looks
like:

```python
maven_jar(
    name = "org_apache_bcel_bcel",
    artifact = "org.apache.bcel:bcel:jar:5.2"
)
```

This would be represented in a `pom.xml` like so (if we were using
one):

```xml
<dependency>
    <groupId>org.apache.bcel</groupId>
    <artifactId>bcel</artifactId>
    <version>5.2</version>
</dependency>
```

Bazel convention states that you should derive and
underscore-delimited list of labels based on the groupId and
artifactId.  It seems redundant / excessive for a small project like
this, but in larger projects we'll need it, so we've gone ahead and
adopted that convention.

### **Gotcha**: Transitive Dependencies

One thing that might be surprising to learn is that
[maven_jar does not compute and automatically include transitive dependencies][bazel-issue-89]
for external maven dependencies.  When I first learned this I was
thinking *WTF!?  No transitive dependencies?  Every other java build
tool has this!  So I have to figure out the dependencies for myself?*.
The answer is yes, you do have to manually explicity name every other
transitive dependency as its own `maven_jar` rule.

I now consider this a feature rather than a bug however.  The problem
with maven is that it's easy to find yourself in a position where you
don't actually really know what your code depends on. After convering
a few real-world projects to bazel, I came away with a much better
understanding of the true nature of my code dependencies and was able
to streamline this into smaller, simpler projects.  Really having to
know your project dependencies is a good thing.

There is also a tool called `generate_workspace` that will do it for
you (I have not used it).  I'd encourage you not to if at all
possible.

In this example repo, bcel has no transitive runtime dependencies, so
we're done.

## Running Tests

TODO.

## Creating a Skylark Rule for bazel-importscrubber

TODO.

## Installing and using third-part bazel rules

TODO.

[bazel-home]: http://www.bazel.io "Bazel Project Page"
[bazel-install]: http://www.bazel.io/docs/install.html "Bazel Install Documentation"
[bazel-workspace-rules]: http://www.bazel.io/docs/be/workspace.html "Bazel Workspace Rules"
[bazel-issue-89]: https://github.com/bazelbuild/bazel/issues/89 "maven_jar does not pull transitive dependencies"
[importscrubber-home]: http://importscrubber.sourceforge.net "ImportScrubber Project Page"
[importscour-home]: https://github.com/davetron5000/importscour "ImportScour Project Page"
[bcel-home]: https://commons.apache.org/proper/commons-bcel/ "Byte Code Engineering Library"
