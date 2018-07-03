package redgatesqlci;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SqlContinuousIntegrationBuilderTest {
    @Mock
    private AbstractBuild<?, ?> abstractBuild;

    @Mock
    private VirtualChannel virtualChannel;

    @Mock
    private Launcher launcher;

    @Mock
    private BuildListener buildListener;

    @Mock
    private Proc process;

    @Before
    public void SetUp() throws IOException {
        when(launcher.getChannel()).thenReturn(virtualChannel);
        when(launcher.launch(any(ProcStarter.class))).thenReturn(process);
    }

    @Test
    public void executeWithMinimalConfigShouldSucceed() throws Throwable {
        when(virtualChannel.call(Matchers.<Callable<?, ? extends Throwable>>any())).thenReturn(true);
        when(abstractBuild.getEnvironment(buildListener)).thenReturn(new EnvVars());
        when(process.join()).thenReturn(0);

        final SqlContinuousIntegrationBuilder sqlContinuousIntegrationBuilder = new SqlContinuousIntegrationBuilder() {
            @Override
            public boolean perform(
                final AbstractBuild<?, ?> build,
                final Launcher launcher,
                final BuildListener listener) {
                return runSqlContinuousIntegrationCmdlet(build, launcher, listener, Collections.singletonList("-someParam someValue"));
            }
        };

        final boolean output = sqlContinuousIntegrationBuilder.perform(abstractBuild, launcher, buildListener);

        assertThat(output, is(true));
    }
}
