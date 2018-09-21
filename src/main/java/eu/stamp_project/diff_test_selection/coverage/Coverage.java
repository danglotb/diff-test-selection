package eu.stamp_project.diff_test_selection.coverage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 21/09/18
 * <p>
 * This class is responsible to compute the Coverage of the provided diff.
 */
public class Coverage {

    private Map<String, List<Integer>> executedLinePerQualifiedName;

    private Map<String, List<Integer>> modifiedLinePerQualifiedName;

    public Coverage() {
        this.modifiedLinePerQualifiedName = new LinkedHashMap<>();
        this.executedLinePerQualifiedName = new LinkedHashMap<>();
    }

    public void covered(String fullQualifiedName, Integer line) {
        if (!this.executedLinePerQualifiedName.containsKey(fullQualifiedName)) {
            this.executedLinePerQualifiedName.put(fullQualifiedName, new ArrayList<>());
        }
        this.executedLinePerQualifiedName.get(fullQualifiedName).add(line);
    }

    public void addModifiedLines(Map<String, List<Integer>> modifiedLinesPerQualifiedName) {
        this.modifiedLinePerQualifiedName.putAll(modifiedLinesPerQualifiedName);
    }

    public Map<String, List<Integer>> getExecutedLinePerQualifiedName() {
        return executedLinePerQualifiedName;
    }

    public Map<String, List<Integer>> getModifiedLinePerQualifiedName() {
        return modifiedLinePerQualifiedName;
    }
}
