package se.denacode.sonar.plugin.teamsnotifier.extension.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import se.denacode.sonar.plugin.teamsnotifier.common.component.ProjectConfig;
import se.denacode.sonar.plugin.teamsnotifier.common.component.ProjectConfigBuilder;
import se.denacode.sonar.plugin.teamsnotifier.extension.task.payload.Fact;
import se.denacode.sonar.plugin.teamsnotifier.extension.task.payload.Payload;
import se.denacode.sonar.plugin.teamsnotifier.extension.task.payload.Section;
import java.util.Locale;
import java.util.Optional;
import org.apache.commons.lang.RandomStringUtils;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.ce.posttask.Branch;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.i18n.DefaultI18n;
import org.sonar.core.platform.PluginRepository;

/**
 * Created by ak on 18/10/16.
 * Modified by poznachowski
 */

public class ProjectAnalysisPayloadBuilderTest {
    private static final Logger LOG = Loggers.get(ProjectAnalysisPayloadBuilderTest.class);
    private static final boolean QG_FAIL_ONLY = true;
    private CaptorPostProjectAnalysisTask postProjectAnalysisTask;
    private DefaultI18n i18n;

    private Locale defaultLocale;

    @Before
    public void before() {
        this.postProjectAnalysisTask = new CaptorPostProjectAnalysisTask();

        // org/sonar/l10n/core.properties
        final PluginRepository pluginRepository = Mockito.mock(PluginRepository.class);
        final System2 system2 = Mockito.mock(System2.class);
        this.i18n = new DefaultI18n(pluginRepository, system2);
        this.i18n.start();

        this.defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @After
    public void after() {
        Locale.setDefault(this.defaultLocale);

    }

    @Test
    public void testI18nBundle() {
        assertThat(this.i18n.message(Locale.ENGLISH, "metric.new_sqale_debt_ratio.short_name", null)).isEqualTo("Debt Ratio on new code");
    }

    @Test
    public void execute_is_passed_a_non_null_ProjectAnalysis_object() {
        Analyses.simple(this.postProjectAnalysisTask);
        assertThat(this.postProjectAnalysisTask.getProjectAnalysis()).isNotNull();
    }

    @Test
    public void testPayloadBuilder() throws JsonProcessingException {
        Analyses.qualityGateOk4Conditions(this.postProjectAnalysisTask);
        final ProjectConfig projectConfig = new ProjectConfigBuilder().setProjectHook("hook")
            .setProjectKeyOrRegExp("key")
            .setNotify("")
            .setQgFailOnly(false).build();
        final Payload payload = ProjectAnalysisPayloadBuilder.of(this.postProjectAnalysisTask.getProjectAnalysis())
            .projectConfig(projectConfig)
            .i18n(this.i18n)
            .projectUrl("http://localhist:9000/dashboard?id=project:key")
            .build();

        String expected = "{\n" +
            "  \"summary\": \"Sonar report\",\n" +
            "  \"themeColor\": \"#00FF00\",\n" +
            "  \"sections\": [\n" +
            "    {\n" +
            "      \"markdown\": true,\n" +
            "      \"facts\": [\n" +
            "        {\n" +
            "          \"name\": \"Quality gate status\",\n" +
            "          \"value\": \"\uD83D\uDFE2 OK\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"name\": \"New Vulnerabilities\",\n" +
            "          \"value\": \"\uD83D\uDFE2 0, error if \\u003e0\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"name\": \"New Bugs\",\n" +
            "          \"value\": \"\uD83D\uDD34 1, error if \\u003e0\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"name\": \"Technical Debt Ratio on New Code\",\n" +
            "          \"value\": \"\uD83D\uDFE2 0.01%, error if \\u003e10.0%\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"name\": \"Coverage on New Code\",\n" +
            "          \"value\": \"\uD83D\uDD34 75.51%, error if \\u003c80.0%\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"activityTitle\": \"Sonar Quality Report\",\n" +
            "      \"activitySubtitle\": \"Project Sonar Project Name was analyzed.\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"potentialAction\": [\n" +
            "    {\n" +
            "      \"@context\": \"http://schema.org\",\n" +
            "      \"@type\": \"ViewAction\",\n" +
            "      \"name\": \"See report\",\n" +
            "      \"target\": [\n" +
            "        \"http://localhist:9000/dashboard?id\\u003dproject:key\"\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        assertThat(gson.toJson(payload)).isEqualTo(expected);
    }

    private Payload expected() {
        return new Payload("Sonar report", new Section("Sonar Quality Report", "Test")); // TODO implement
    }

    @Test
    public void shouldShowOnlyExceededConditionsIfProjectConfigReportOnlyOnFailedQualityGateWay() {
        Analyses.qualityGateError2Of3ConditionsFailed(this.postProjectAnalysisTask);
        final ProjectConfig projectConfig = new ProjectConfigBuilder()
            .setProjectHook("hook")
            .setProjectKeyOrRegExp("key")
            .setNotify("")
            .setQgFailOnly(QG_FAIL_ONLY).build();
        final Payload payload = ProjectAnalysisPayloadBuilder.of(this.postProjectAnalysisTask.getProjectAnalysis())
            .projectConfig(projectConfig)
            .i18n(this.i18n)
            .projectUrl("http://localhist:9000/dashboard?id=project:key")
            .build();

        assertThat(payload.getSection().getFacts())
            .flatExtracting(Fact::getName)
            .contains("Functions", "Issues");
        assertThat(payload.getSection().getFacts())
            .flatExtracting(Fact::getValue)
            .contains("\uD83D\uDFE0 1, error if >0", "\uD83D\uDD34 10, error if >5");
    }

    @Test
    public void buildPayloadWithoutQualityGateWay() {
        Analyses.noQualityGate(this.postProjectAnalysisTask);
        final ProjectConfig projectConfig = new ProjectConfigBuilder()
            .setProjectHook("hook")
            .setProjectKeyOrRegExp("key")
            .setNotify("")
            .setQgFailOnly(false).build();
        final Payload payload = ProjectAnalysisPayloadBuilder.of(this.postProjectAnalysisTask.getProjectAnalysis())
            .projectConfig(projectConfig)
            .i18n(this.i18n)
            .projectUrl("http://localhist:9000/dashboard?id=project:key")
            .build();

        assertThat(payload.getSection().getActivityTitle()).doesNotContain("Quality Gate status");
    }

    @Test
    public void buildPayloadWithoutNotify() {
        Analyses.noQualityGate(this.postProjectAnalysisTask);
        final ProjectConfig projectConfig = new ProjectConfigBuilder().setProjectHook("key").setProjectKeyOrRegExp("#channel")
            .setNotify("").setQgFailOnly(false)
            .build();
        final Payload payload = ProjectAnalysisPayloadBuilder.of(this.postProjectAnalysisTask.getProjectAnalysis())
            .projectConfig(projectConfig)
            .i18n(this.i18n)
            .projectUrl("http://localhist:9000/dashboard?id=project:key")
            .build();

        assertThat(payload.getSection().getActivityTitle()).doesNotContain("here");
    }

    @Test
    public void build_noBranch_notifyPrefixAppended() {
        Analyses.noQualityGate(this.postProjectAnalysisTask);
        final String notify = RandomStringUtils.random(10);
        final Payload payload = ProjectAnalysisPayloadBuilder.of(this.postProjectAnalysisTask.getProjectAnalysis())
            .projectConfig(new ProjectConfigBuilder().setProjectHook("hook").setProjectKeyOrRegExp("kew")
                .setNotify(notify).setQgFailOnly(false).build())
            .i18n(this.i18n)
            .projectUrl("http://localhost:9000/dashboard?id=project:key")
            .build();
        Assert.assertEquals(String.format("<!%s> Project Sonar Project Name was analyzed.",
            notify), payload.getSection().getActivitySubtitle());
    }

    @Test
    public void build_mainBranch_branchNotAddedToMessage() {

        final String branchName = "my-branch";
        final boolean isMain = true;

        Analyses.withBranch(this.postProjectAnalysisTask, this.newBranch(branchName, isMain));
        final Payload payload = ProjectAnalysisPayloadBuilder.of(this.postProjectAnalysisTask.getProjectAnalysis())
            .projectConfig(new ProjectConfigBuilder().setProjectHook("key").setProjectKeyOrRegExp("#channel")
                .setNotify("").setQgFailOnly(false).build())
            .i18n(this.i18n)
            .projectUrl("http://localhost:9000/dashboard?id=project:key")
            .includeBranch(true)
            .build();
        Assert.assertEquals("Project Sonar Project Name was analyzed.", payload.getSection().getActivitySubtitle());
    }

    @Test
    public void build_withIncludeBranchTrue_branchAddedToMessage() {

        final String branchName = RandomStringUtils.random(10);
        Analyses.withBranch(this.postProjectAnalysisTask, this.newBranch(branchName, false));
        final Payload payload = ProjectAnalysisPayloadBuilder.of(this.postProjectAnalysisTask.getProjectAnalysis())
            .projectConfig(new ProjectConfigBuilder().setProjectHook("key").setProjectKeyOrRegExp("#channel")
                .setNotify("").setQgFailOnly(false).build())
            .i18n(this.i18n)
            .projectUrl("http://localhost:9000/dashboard?id=project:key")
            .includeBranch(true)
            .build();
        Assert.assertEquals(String.format("Project Sonar Project Name was analyzed for branch %s.",
            branchName), payload.getSection().getActivitySubtitle());
    }

    @Test
    public void build_enabledButNoBranchPresent_branchNotAddedToMessage() {

        Analyses.simple(this.postProjectAnalysisTask);
        final Payload payload = ProjectAnalysisPayloadBuilder.of(this.postProjectAnalysisTask.getProjectAnalysis())
            .projectConfig(new ProjectConfigBuilder().setProjectHook("key").setProjectKeyOrRegExp("#channel")
                .setNotify("").setQgFailOnly(false).build())
            .i18n(this.i18n)
            .projectUrl("http://localhost:9000/dashboard?id=project:key")
            .includeBranch(true)
            .build();
        Assert.assertEquals("Project Sonar Project Name was analyzed.",
            payload.getSection().getActivitySubtitle());
        assertThat(payload.getSection().getFacts().get(0).getName()).contains("Quality gate status");
        assertThat(payload.getSection().getFacts().get(0).getValue()).contains("OK");
    }

    private Branch newBranch(final String name, final boolean isMain) {

        return new Branch() {

            @Override
            public boolean isMain() {

                return isMain;
            }

            @Override
            public Optional<String> getName() {

                return Optional.of(name);
            }

            @Override
            public Type getType() {

                return Type.BRANCH;
            }
        };
    }
}
