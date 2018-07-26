package eu.stamp_project.diff_test_selection.report;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.logging.Log;

import java.util.List;
import java.util.Map;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 26/07/18
 */
public interface Report {

    /**
     * this method should out put the result in the given path
     * @param logger instance of the plugin logger (should use {@link Mojo#getLog()}
     * @param outputPath path out the report
     * @param testThatExecuteChanges map that associates full qualified name
     */
    void report(
            final Log logger,
            final String outputPath,
            final Map<String, List<String>> testThatExecuteChanges
    );

}
