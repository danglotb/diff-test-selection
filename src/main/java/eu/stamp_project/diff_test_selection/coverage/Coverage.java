package eu.stamp_project.diff_test_selection.coverage;

import java.util.*;

/**
 * created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 21/09/18
 * <p>
 * This class is responsible to compute the Coverage of the provided diff.
 */
public class Coverage {

    private Map<String, Set<Integer>> executedLinePerQualifiedName;

    private Map<String, Set<Integer>> modifiedLinePerQualifiedName;

    public Coverage() {
        this.modifiedLinePerQualifiedName = new LinkedHashMap<>();
        this.executedLinePerQualifiedName = new LinkedHashMap<>();
    }

    public void covered(String fullQualifiedName, Integer line) {
        if (!this.executedLinePerQualifiedName.containsKey(fullQualifiedName)) {
            this.executedLinePerQualifiedName.put(fullQualifiedName, new HashSet<>());
        }
        this.executedLinePerQualifiedName.get(fullQualifiedName).add(line);
    }

    public void addModifiedLines(Map<String, List<Integer>> modifiedLinesPerQualifiedName) {
        modifiedLinesPerQualifiedName.keySet()
                .forEach(
                        key -> this.modifiedLinePerQualifiedName.put(key, new HashSet<>(modifiedLinesPerQualifiedName.get(key)))
                );
    }

    public Map<String, Set<Integer>> getExecutedLinePerQualifiedName() {
        return executedLinePerQualifiedName;
    }

    public Map<String, Set<Integer>> getModifiedLinePerQualifiedName() {
        return modifiedLinePerQualifiedName;
    }
}
