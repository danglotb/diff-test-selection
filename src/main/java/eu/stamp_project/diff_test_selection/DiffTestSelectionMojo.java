package eu.stamp_project.diff_test_selection;

import eu.stamp_project.diff_test_selection.report.CSVReport;
import eu.stamp_project.diff_test_selection.report.Report;
import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.Operation;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spoon.reflect.cu.CompilationUnit;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mojo(name = "list")
public class DiffTestSelectionMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true)
    private MavenProject project;

    @Parameter(property = "pathToDiff", required = true)
    private String pathToDiff;

    @Parameter(property = "pathToOtherVersion", required = true)
    private String pathToOtherVersion;

    @Parameter(property = "outputPath", defaultValue = "testsThatExecuteTheChange.csv")
    private String outputPath;

    @Parameter(property = "report", defaultValue = "CSV")
    private String report;

    private enum ReportEnum {
        CSV(new CSVReport());
        public final Report instance;
        ReportEnum(Report instance) {
            this.instance = instance;
        }
    }

    private void checksArguments() {
        this.pathToOtherVersion = checksIfExistAndUseAbsolutePath(this.pathToOtherVersion) + "/";
        this.pathToDiff = checksIfExistAndUseAbsolutePath(this.pathToDiff) + "/";
    }

    private String checksIfExistAndUseAbsolutePath(String pathFileToCheck) {
        final File file = new File(pathFileToCheck);
        if (!file.exists()) {
            getLog().error(pathFileToCheck + " does not exist, please check it out!", new IllegalArgumentException(pathFileToCheck));
            return ""; // make javac happy
        } else {
            return file.getAbsolutePath();
        }
    }

    public void execute() throws MojoExecutionException {
        checksArguments();
        final File baseDir = project.getBasedir();
        getLog().info(baseDir.getAbsolutePath());
        final Map<String, Map<String, Map<String, List<Integer>>>> coverage = getCoverage(baseDir);
        final Map<String, List<String>> testThatExecuteChanges = this.getTestThatExecuteChanges(coverage);
        ReportEnum.valueOf(this.report).instance.report(
                        getLog(),
                        baseDir.getAbsolutePath() + "/" + this.outputPath,
                        testThatExecuteChanges
                );
    }

    private Map<String, Map<String, Map<String, List<Integer>>>> getCoverage(File basedir) {
//        new CloverExecutor().instrumentAndRunTest(basedir.getAbsolutePath());
        return new CloverReader().read(basedir.getAbsolutePath());
    }

    private Map<String, List<String>> getTestThatExecuteChanges(Map<String, Map<String, Map<String, List<Integer>>>> coverage) {
        final Map<String, List<String>> testMethodPerTestClasses = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(pathToDiff)))) {
            String currentLine = null;
            while ((currentLine = reader.readLine()) != null) {
                if (currentLine.startsWith("+++") || currentLine.startsWith("---")) {
                    Map<String, List<Integer>> modifiedLinesPerQualifiedName =
                            getModifiedLinesPerQualifiedName(pathToOtherVersion, currentLine, reader.readLine());
                    if (modifiedLinesPerQualifiedName == null) {
                        continue;
                    }
                    testMethodPerTestClasses.putAll(matchChangedWithCoverage(coverage, modifiedLinesPerQualifiedName));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return testMethodPerTestClasses;
    }

    @Nullable
    private Map<String, List<Integer>> getModifiedLinesPerQualifiedName(String pathToOtherVersion,
                                                                        String currentLine,
                                                                        String secondLine) throws Exception {
        final File baseDir = project.getBasedir();
        String file1 = getCorrectPathFile(currentLine);
        String file2 = getCorrectPathFile(secondLine);
        if (!file1.equals(file2)) {
            System.out.println("Could not match " + file1 + " and " + file2);
            return null;
        }
        final Diff compare = new AstComparator()
                .compare(
                        new File(baseDir + file1),
                        new File(pathToOtherVersion + file2)
                );
        return buildMap(compare);
    }

    @NotNull
    private Map<String, List<Integer>> buildMap(Diff compare) {
        Map<String, List<Integer>> modifiedLinesPerQualifiedName = new HashMap<>();
        for (Operation operation : compare.getAllOperations()) {
            final CtElement srcNode = operation.getSrcNode();
            if (srcNode == null) {
                continue;
            }
            final SourcePosition position = srcNode.getPosition();
            if (position == null) {
                continue;
            }
            final CompilationUnit compilationUnit = position.getCompilationUnit();
            if (compilationUnit == null) {
                continue;
            }
            final CtType<?> mainType = compilationUnit.getMainType();
            if (mainType == null) {
                continue;
            }
            final String qualifiedName = mainType.getQualifiedName();
            if (!modifiedLinesPerQualifiedName.containsKey(qualifiedName)) {
                modifiedLinesPerQualifiedName.put(qualifiedName, new ArrayList<>());
            }
            modifiedLinesPerQualifiedName.get(qualifiedName).add(position.getLine());
        }
        return modifiedLinesPerQualifiedName;
    }

    private Map<String, List<String>> matchChangedWithCoverage(Map<String, Map<String, Map<String, List<Integer>>>> coverage,
                                                               Map<String, List<Integer>> modifiedLinesPerQualifiedName) {
        Map<String, List<String>> collect = coverage
                .keySet()
                .stream()
                .collect(Collectors.toMap(
                        testClassKey -> testClassKey,
                        testClassKey ->
                                coverage.get(testClassKey)
                                        .keySet()
                                        .stream()
                                        .filter(testMethodKey ->
                                                coverage.get(testClassKey)
                                                        .get(testMethodKey)
                                                        .keySet()
                                                        .stream()
                                                        .filter(modifiedLinesPerQualifiedName::containsKey)
                                                        .anyMatch(targetClassName ->
                                                                modifiedLinesPerQualifiedName.get(targetClassName)
                                                                        .stream()
                                                                        .anyMatch(line ->
                                                                                coverage.get(testClassKey)
                                                                                        .get(testMethodKey)
                                                                                        .get(targetClassName).contains(line)
                                                                        )
                                                        )
                                        ).collect(Collectors.toList())
                        )
                );
        for (String key : new ArrayList<>(collect.keySet())) {
            if (collect.get(key).isEmpty()) {
                collect.remove(key);
            }
        }
        return collect;
    }

    private String getCorrectPathFile(String path) {
        final String s = path.split(" ")[1];
        if (s.contains("\t")) {
            return s.split("\t")[0].substring(1);
        }
        return s.substring(1); // removing the first letter, i.e. a and b
    }

    /*
     *  Test purposes
     */

    void setProject(MavenProject project) {
        this.project = project;
    }

    void setPathToDiff(String pathToDiff) {
        this.pathToDiff = pathToDiff;
    }

    void setPathToOtherVersion(String pathToOtherVersion) {
        this.pathToOtherVersion = pathToOtherVersion;
    }

    void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    void setReport(String report) {
        this.report = report;
    }

    public static void main(String[] args) {
        DiffTestSelectionMojo diffTestSelectionMojo = new DiffTestSelectionMojo();
        diffTestSelectionMojo.setPathToDiff("/home/bdanglot/workspace/bugs-dot-jar/commons-math/.bugs-dot-jar/developer-patch.diff");
        diffTestSelectionMojo.setPathToOtherVersion("/home/bdanglot/workspace/bugs-dot-jar/commons-math_fixed");
        MavenProject mavenProject = new MavenProject();
        mavenProject.setPomFile(new File("/home/bdanglot/workspace/bugs-dot-jar/commons-math/pom.xml"));
        mavenProject.setFile(new File("/home/bdanglot/workspace/bugs-dot-jar/commons-math/pom.xml"));
        diffTestSelectionMojo.setProject(mavenProject);
        diffTestSelectionMojo.setLog(new DefaultLog(new ConsoleLogger(0, "logger")));
        diffTestSelectionMojo.setReport("CSV");
        diffTestSelectionMojo.setOutputPath("testsThatExecuteTheChange.csv");
        try {
            diffTestSelectionMojo.execute();
        } catch (MojoExecutionException e) {
            e.printStackTrace();
        }
    }

}