replace_java:
	rpl -vvR -x'.java' 'net.sourceforge.importscrubber' 'org.pubref.util.importscrubber' src/main/java

clang_format:
	find src/main/java -name '*.java' \
	| xargs java -jar ~/bin/google-java-format-0.1-alpha.jar --replace

lib:
	~/bin/bazel build //src/main/java/org/pubref/util/importscrubber:importscrubber

jar:
	~/bin/bazel build //src/main/java/org/pubref/util/importscrubber:importscrubber_bin_deploy.jar
