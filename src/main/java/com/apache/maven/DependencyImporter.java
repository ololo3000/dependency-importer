package com.apache.maven;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.swing.JOptionPane;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;

import static org.codehaus.plexus.util.StringUtils.isBlank;

/** */
public class DependencyImporter {
    /** */
    private static final int POM_PATH = 0;

    /** */
    private static final int JAR_PATH = 1;

    /** */
    public static void main(String[] args) {
        if (args.length < 1)
            throw new RuntimeException();

        File dir = new File(args[0]);

        Map<String, String[]> dependencies = new HashMap<>();

        extractDependencies(dependencies, dir);

        importDependencies(dependencies);
    }

    /** */
    private static void importDependencies(Map<String, String[]> dependencies) {
        Objects.requireNonNull(dependencies);

        String mvnHomePath = System.getenv("M2_HOME");

        if (isBlank(mvnHomePath))
            throw new RuntimeException();

        dependencies.forEach((name, info) -> {
            String pomPath = info[POM_PATH];

            String jarPath = info[JAR_PATH];

            if (isBlank(pomPath) || isBlank(jarPath))
                return;

            InvocationRequest req = new DefaultInvocationRequest();

            req.setGoals(Collections.singletonList("install:install-file"));
            req.setMavenOpts("-Dfile=" + jarPath + " -DpomFile=" + pomPath);
            req.setBaseDirectory(new File(mvnHomePath));

            Invoker invoker = new DefaultInvoker();

            try {
                invoker.execute(req);

                JOptionPane.showMessageDialog(null, "Library installed " + pomPath + ", " + jarPath);
            }
            catch (Exception e) {
                JOptionPane.showMessageDialog(null, e.getStackTrace(), "Install Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    /** */
    private static void extractDependencies(Map<String, String[]> dependencies, File dir) {
        if (dir == null || !dir.isDirectory())
            throw new RuntimeException();

        File[] files = dir.listFiles();

        if (files == null)
            throw new RuntimeException();

        for (File file : files) {
            if (file.isDirectory())
                extractDependencies(dependencies, file);

            String ext = FilenameUtils.getExtension(file.getName());

            String name = FilenameUtils.getBaseName(file.getName());

            String[] dependency = dependencies.computeIfAbsent(name, k -> new String[2]);

            if ("pom".equals(ext))
                dependency[POM_PATH] = file.getAbsolutePath();
            else if ("jar".equals(ext))
                dependency[JAR_PATH] = file.getAbsolutePath();
        }
    }
}
