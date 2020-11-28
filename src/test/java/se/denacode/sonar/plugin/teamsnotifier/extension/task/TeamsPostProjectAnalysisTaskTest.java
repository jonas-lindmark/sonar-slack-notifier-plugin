package se.denacode.sonar.plugin.teamsnotifier.extension.task;

import se.denacode.sonar.plugin.teamsnotifier.common.TeamsNotifierProp;
import static se.denacode.sonar.plugin.teamsnotifier.extension.task.Analyses.PROJECT_KEY;
import se.denacode.sonar.plugin.teamsnotifier.extension.task.payload.Payload;
import java.io.IOException;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.config.Settings;
import org.sonar.api.config.internal.ConfigurationBridge;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.i18n.I18n;

/**
 * Created by 616286 on 3.6.2016.
 * Modified by poznachowski
 */
public class TeamsPostProjectAnalysisTaskTest {

    private static final String HOOK = "http://hook";
    private static final String DIFFERENT_KEY = "different:key";

    private CaptorPostProjectAnalysisTask postProjectAnalysisTask;

    private TeamsPostProjectAnalysisTask task;

    private OfficeWebhookClient httpClient;

    private MapSettings settings;
    private I18n i18n;

    @Before
    public void before() throws IOException {
        this.postProjectAnalysisTask = new CaptorPostProjectAnalysisTask();
        this.settings = new MapSettings();
        this.settings.setProperty(TeamsNotifierProp.ENABLED.property(), "true");
        this.settings.setProperty(TeamsNotifierProp.HOOK.property(), HOOK);
        this.settings.setProperty(TeamsNotifierProp.CHANNEL.property(), "channel");
        this.settings.setProperty(TeamsNotifierProp.ICON_URL.property(), "");
        this.settings.setProperty(TeamsNotifierProp.PROXY_IP.property(), "127.0.0.1");
        this.settings.setProperty(TeamsNotifierProp.PROXY_PORT.property(), "8080");
        this.settings.setProperty(TeamsNotifierProp.PROXY_PROTOCOL.property(), "http");
        this.settings.setProperty(TeamsNotifierProp.CONFIG.property(), PROJECT_KEY);
        this.settings.setProperty(TeamsNotifierProp.CONFIG.property() + "." + PROJECT_KEY + "." + TeamsNotifierProp.PROJECT_REGEXP.property(), PROJECT_KEY);
        this.settings.setProperty(TeamsNotifierProp.CONFIG.property() + "." + PROJECT_KEY + "." + TeamsNotifierProp.CHANNEL.property(), "#random");
        this.settings.setProperty(TeamsNotifierProp.CONFIG.property() + "." + PROJECT_KEY + "." + TeamsNotifierProp.QG_FAIL_ONLY.property(), "false");
        this.settings.setProperty("sonar.core.serverBaseURL", "http://your.sonar.com/");
        this.httpClient = mock(OfficeWebhookClient.class);
        this.i18n = mock(I18n.class);
        when(this.i18n.message(ArgumentMatchers.any(Locale.class), anyString(), anyString())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(final InvocationOnMock invocation) throws Throwable {
                return (String) invocation.getArguments()[2];
            }
        });

        this.task = new TeamsPostProjectAnalysisTask(this.httpClient, new ConfigurationBridge(this.settings), this.i18n);
    }

    @Test
    public void shouldCall() throws Exception {
        Analyses.simple(this.postProjectAnalysisTask);
        when(this.httpClient.invokeIncomingWebhook(ArgumentMatchers.eq(HOOK), isA(Payload.class))).thenReturn(true);
        this.task.finished(context(this.postProjectAnalysisTask.getProjectAnalysis()));
        verify(this.httpClient, times(1)).invokeIncomingWebhook(ArgumentMatchers.eq(HOOK), isA(Payload.class));
    }

    private PostProjectAnalysisTask.Context context(PostProjectAnalysisTask.ProjectAnalysis projectAnalysis) {
        return new PostProjectAnalysisTask.Context() {
            @Override
            public PostProjectAnalysisTask.ProjectAnalysis getProjectAnalysis() {
                return projectAnalysis;
            }

            @Override
            public PostProjectAnalysisTask.LogStatistics getLogStatistics() {
                return null;
            }
        };
    }

    @Test
    public void shouldSkipIfPluginDisabled() throws Exception {
        this.settings.setProperty(TeamsNotifierProp.ENABLED.property(), "false");
        Analyses.simple(this.postProjectAnalysisTask);
        this.task.finished(context(this.postProjectAnalysisTask.getProjectAnalysis()));
        verifyNoInteractions(this.httpClient);
    }

    @Test
    public void shouldSkipIfReportFailedQualityGateButOk() throws Exception {
        this.settings.setProperty(TeamsNotifierProp.CONFIG.property() + "." + PROJECT_KEY + "." + TeamsNotifierProp.QG_FAIL_ONLY.property(), "true");
        Analyses.simple(this.postProjectAnalysisTask);
        this.task.finished(context(this.postProjectAnalysisTask.getProjectAnalysis()));
        verifyNoInteractions(this.httpClient);
    }
}
