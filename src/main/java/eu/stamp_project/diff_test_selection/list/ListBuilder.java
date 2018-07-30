package eu.stamp_project.diff_test_selection.list;

import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.Operation;
import org.apache.maven.plugin.logging.Log;
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
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 30/07/18
 */
public class ListBuilder {

    private String pathToDiff;

    private String pathToOtherVersion;

    private File baseDir;

    private Log log;

    public ListBuilder(String pathToDiff, String pathToOtherVersion, File baseDir, Log log) {
        this.pathToDiff = pathToDiff;
        this.pathToOtherVersion = pathToOtherVersion;
        this.baseDir = baseDir;
        this.log = log;
    }

    public Map<String, List<String>> getTestThatExecuteChanges(Map<String, Map<String, Map<String, List<Integer>>>> coverage) {
        final Map<String, List<String>> testMethodPerTestClasses = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(this.pathToDiff)))) {
            String currentLine = null;
            while ((currentLine = reader.readLine()) != null) {
                if (currentLine.startsWith("+++") || currentLine.startsWith("---")) {
                    Map<String, List<Integer>> modifiedLinesPerQualifiedName =
                            getModifiedLinesPerQualifiedName(this.pathToOtherVersion, currentLine, reader.readLine());
                    if (modifiedLinesPerQualifiedName == null) {
                        continue;
                    }
                    Map<String, List<String>> matchedChangedWithCoverage = matchChangedWithCoverage(coverage, modifiedLinesPerQualifiedName);
                    matchedChangedWithCoverage.keySet().forEach(key -> {
                        if (!testMethodPerTestClasses.containsKey(key)) {
                            testMethodPerTestClasses.put(key, matchedChangedWithCoverage.get(key));
                        } else {
                            testMethodPerTestClasses.get(key).addAll(matchedChangedWithCoverage.get(key));
                        }
                    });
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
        final String file1 = getCorrectPathFile(currentLine);
        final String file2 = getCorrectPathFile(secondLine);
        if (!file1.equals(file2)) {
            this.log.warn("Could not match " + file1 + " and " + file2);
            return null;
        }
        try {
            final File f1 = getCorrectFile(this.baseDir.getAbsolutePath(), file1);
            final File f2 = getCorrectFile(pathToOtherVersion, file2);
            return buildMap(new AstComparator().compare(f1, f2));
        } catch (Exception e) {
            this.log.error("Error when trying to compare " + file1 + " and " + file2);
            return null;
        }
    }

    private File getCorrectFile(String baseDir, String fileName){
        final File file = new File(baseDir + "/" + fileName);
        return file.exists() ? file : new File(baseDir + "/../" + fileName);
    }

    @NotNull
    private Map<String, List<Integer>> buildMap(Diff compare) {
        Map<String, List<Integer>> modifiedLinesPerQualifiedName = new LinkedHashMap<>();// keeps the order
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
}
