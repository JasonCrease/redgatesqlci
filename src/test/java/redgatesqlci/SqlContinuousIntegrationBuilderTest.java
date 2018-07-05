package redgatesqlci;

import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SqlContinuousIntegrationBuilderTest {
    private AbstractBuild<?, ?> abstractBuild;

    @Mock
    private Launcher launcher;

    @Mock
    private BuildListener buildListener;

    @Mock
    private Proc process;
    private File testFolder;

    @Before
    public void SetUp() throws IOException {
        testFolder = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        FileUtils.forceMkdir(testFolder);

        abstractBuild = new MockBuild(testFolder);
        when(launcher.launch(any(ProcStarter.class))).thenReturn(process);
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(testFolder);
    }

    @Test
    public void executeWithMinimalConfigShouldSucceed() throws IOException, InterruptedException {
        when(process.join()).thenReturn(0);

        final SqlContinuousIntegrationBuilder sqlContinuousIntegrationBuilder = new SqlContinuousIntegrationBuilder() {
            @Override
            public boolean perform(
                final AbstractBuild<?, ?> build,
                final Launcher launcher,
                final BuildListener listener) {
                return runSqlContinuousIntegrationCmdlet(
                    build,
                    launcher,
                    listener,
                    Collections.singletonList("-someParam someValue"));
            }
        };

        final boolean output = sqlContinuousIntegrationBuilder.perform(
            abstractBuild,
            launcher,
            buildListener);

        assertThat(output, is(true));
    }
}
