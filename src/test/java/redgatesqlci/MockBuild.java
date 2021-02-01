package redgatesqlci;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.model.TaskListener;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockBuild extends FreeStyleBuild {
    private final Node node;

    MockBuild(final File testFolder) throws IOException {
        super(mock(FreeStyleProject.class));

        node = mock(Node.class);
        final FilePath workspace = new FilePath(testFolder);
        when(node.createPath(anyString())).thenReturn(workspace);
        setWorkspace(workspace);
    }

    @Override
    public void run() {
    }

    @Nonnull
    @Override
    public EnvVars getEnvironment(final TaskListener log) {
        return new EnvVars();
    }

    @Override
    public Map<String, String> getBuildVariables() {
        return Collections.emptyMap();
    }

    @CheckForNull
    @Override
    public Node getBuiltOn() {
        return node;
    }
}
