package eu.stamp_project.diff_test_selection;

import eu.stamp_project.diff_test_selection.clover.CloverExecutor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 30/07/18
 *
 * This mojo will first call the following goals:
 *  clean org.openclover:clover-maven-plugin:4.2.0:setup test then applies the plugin {@link DiffTestSelectionMojo}:list.
 *  However, this is not recommended, since the pre goal will take more times than running yourself the goals then the {@link DiffTestSelectionMojo}:list,
 *  <i>i.e.</i>, run mvn clean org.openclover:clover-maven-plugin:4.2.0:setup test eu.stamp-project:diff-test-selection:list rather than
 *  mvn eu.stamp-project:diff-test-selection:instrumentAndList
 *
 */
@Mojo(name = "instrumentAndList")
public class DiffTestSelectionWithInstrumentationMojo extends DiffTestSelectionMojo {

    @Override
    public void execute() throws MojoExecutionException {
        final File baseDir = project.getBasedir();
        final CloverExecutor cloverExecutor = new CloverExecutor();
        cloverExecutor.instrumentAndRunTest(baseDir.getAbsolutePath());
        super.execute();
    }
}
