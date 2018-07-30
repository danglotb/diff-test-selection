package eu.stamp_project.diff_test_selection;

import eu.stamp_project.diff_test_selection.clover.CloverExecutor;
import eu.stamp_project.diff_test_selection.clover.CloverReader;
import eu.stamp_project.diff_test_selection.list.ListBuilder;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This mojo will build from the clover result, the list of test classes and test methods that execute the given changes.
 * Before running this plugins, you should the instrumentation by clover then the test, <i>i.e.</i>
 * mvn clean clean org.openclover:clover-maven-plugin:4.2.0:setup test eu.stamp-project:diff-test-selection:list
 */
@Mojo(name = "list")
public class DiffTestSelectionMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true)
    protected MavenProject project;

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
            System.exit(1);
        }
        return file.getAbsolutePath();
    }

    public void execute() throws MojoExecutionException {
        checksArguments();
        final File baseDir = project.getBasedir();
        getLog().info(baseDir.getAbsolutePath());
        final Map<String, Map<String, Map<String, List<Integer>>>> coverage = getCoverage(baseDir);
        final ListBuilder listBuilder = new ListBuilder(this.pathToDiff, this.pathToOtherVersion, baseDir, getLog());
        final Map<String, List<String>> testThatExecuteChanges = listBuilder.getTestThatExecuteChanges(coverage);
        ReportEnum.valueOf(this.report).instance.report(
                getLog(),
                baseDir.getAbsolutePath() + "/" + this.outputPath,
                testThatExecuteChanges
        );
    }

    private Map<String, Map<String, Map<String, List<Integer>>>> getCoverage(File basedir) {
        new CloverExecutor().instrumentAndRunTest(basedir.getAbsolutePath());
        return new CloverReader().read(basedir.getAbsolutePath());
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