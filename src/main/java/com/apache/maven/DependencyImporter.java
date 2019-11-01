package com.apache.maven;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;

import static org.apache.maven.shared.utils.StringUtils.isBlank;

/** */
public class DependencyImporter {
    /** */
    private static final int POM_PATH = 0;

    /** */
    private static final int JAR_PATH = 1;

    /** */
    private static final String POM_EXTENSION = "pom";

    /** */
    private static final  String JAR_EXTENSION = "jar";

    /** */
    public static void main(String[] args) {
        if (args.length < 1) {
            throw new RuntimeException("As an argument to the program, it's required the path to the folder from" +
                " which all decent libraries will be recursively imported.");
        }

        File dir = new File(args[0]);

        Map<String, String[]> dependencies = new HashMap<>();

        extractDependencies(dependencies, dir);

        importDependencies(dependencies);
    }

    /** */
    private static void importDependencies(Map<String, String[]> dependencies) {
        Objects.requireNonNull(dependencies);

        String mvnHomePath = System.getenv("M2_HOME");

        if (isBlank(mvnHomePath)) {
            throw new RuntimeException("The M2_HOME property must be set as an environment property since it's used " +
                "as the path to local repository folder.");
        }

        dependencies.forEach((name, info) -> {
            String pomPath = info[POM_PATH];

            String jarPath = info[JAR_PATH];

            if (isBlank(pomPath) && isBlank(jarPath))
                return;

            InvocationRequest req = new DefaultInvocationRequest();

            String mvnOpts;

            if (isBlank(jarPath))
                mvnOpts = "-Dfile=" + pomPath + " -DpomFile=" + pomPath + " -Dpackaging=pom" ;
            else if (isBlank(pomPath))
                mvnOpts = "-Dfile=" + jarPath;
            else
                mvnOpts = "-Dfile=" + jarPath + " -DpomFile=" + pomPath;

            req.setGoals(Collections.singletonList("org.apache.maven.plugins:maven-install-plugin:3.0.0-M1:install-file"));
            req.setMavenOpts(mvnOpts);
            req.setBaseDirectory(new File(mvnHomePath));

            Invoker invoker = new DefaultInvoker();

            System.err.println(name + " library installing...");

            try {
                invoker.execute(req);
            }
            catch (Exception e) {
                System.err.println("Exception occurred while " + name + "processing " +
                    "[pomPath=" + pomPath + ", jarPath=" + jarPath + ']' + e);
            }
        });
    }

    /** */
    private static void extractDependencies(Map<String, String[]> dependencies, File dir) {
        if (dir == null || !dir.isDirectory())
            throw new RuntimeException();

        File[] files = dir.listFiles();

        for (File file : files) {
            if (file.isDirectory())
                extractDependencies(dependencies, file);

            String ext = FilenameUtils.getExtension(file.getName());

            String name = FilenameUtils.getBaseName(file.getName());

            String[] dependency = dependencies.computeIfAbsent(name, k -> new String[2]);

            if (POM_EXTENSION.equals(ext))
                dependency[POM_PATH] = file.getAbsolutePath();
            else if (JAR_EXTENSION.equals(ext))
                dependency[JAR_PATH] = file.getAbsolutePath();
        }
    }
}
