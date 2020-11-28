package se.denacode.sonar.plugin.teamsnotifier.extension.task;

import se.denacode.sonar.plugin.teamsnotifier.common.component.ProjectConfig;
import se.denacode.sonar.plugin.teamsnotifier.extension.task.payload.Action;
import se.denacode.sonar.plugin.teamsnotifier.extension.task.payload.Fact;
import se.denacode.sonar.plugin.teamsnotifier.extension.task.payload.Payload;
import se.denacode.sonar.plugin.teamsnotifier.extension.task.payload.Section;
import static java.lang.String.format;
import java.text.DecimalFormat;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import org.sonar.api.ce.posttask.Branch;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.i18n.I18n;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

class ProjectAnalysisPayloadBuilder {
    private static final Logger LOG = Loggers.get(ProjectAnalysisPayloadBuilder.class);

    private static final Map<QualityGate.Status, String> statusSymbol = new EnumMap<>(QualityGate.Status.class);
    private static final Map<QualityGate.EvaluationStatus, String> evaluationStatusSymbol = new EnumMap<>(QualityGate.EvaluationStatus.class);
    static {
        statusSymbol.put(QualityGate.Status.OK, "\uD83D\uDFE2");
        statusSymbol.put(QualityGate.Status.WARN, "\uD83D\uDFE0");
        statusSymbol.put(QualityGate.Status.ERROR, "\uD83D\uDD34");
        evaluationStatusSymbol.put(QualityGate.EvaluationStatus.NO_VALUE, "⚪️️");
        evaluationStatusSymbol.put(QualityGate.EvaluationStatus.OK, "\uD83D\uDFE2");
        evaluationStatusSymbol.put(QualityGate.EvaluationStatus.WARN, "\uD83D\uDFE0");
        evaluationStatusSymbol.put(QualityGate.EvaluationStatus.ERROR, "\uD83D\uDD34");
    }

    private final PostProjectAnalysisTask.ProjectAnalysis analysis;
    private final DecimalFormat percentageFormat;
    private I18n i18n;
    private ProjectConfig projectConfig;
    private String projectUrl;
    private boolean includeBranch;

    private ProjectAnalysisPayloadBuilder(final PostProjectAnalysisTask.ProjectAnalysis analysis) {
        this.analysis = analysis;
        // Format percentages as 25.01 instead of 25.0066666666666667 etc.
        this.percentageFormat = new DecimalFormat();
        this.percentageFormat.setMaximumFractionDigits(2);
    }

    static ProjectAnalysisPayloadBuilder of(final PostProjectAnalysisTask.ProjectAnalysis analysis) {
        return new ProjectAnalysisPayloadBuilder(analysis);
    }

    ProjectAnalysisPayloadBuilder projectConfig(final ProjectConfig projectConfig) {
        this.projectConfig = projectConfig;
        return this;
    }

    ProjectAnalysisPayloadBuilder i18n(final I18n i18n) {
        this.i18n = i18n;
        return this;
    }

    ProjectAnalysisPayloadBuilder projectUrl(final String projectUrl) {
        this.projectUrl = projectUrl;
        return this;
    }

    ProjectAnalysisPayloadBuilder includeBranch(final boolean includeBranch) {
        this.includeBranch = includeBranch;
        return this;
    }

    Payload build() {
        assertNotNull(projectConfig, "projectConfig");
        assertNotNull(projectUrl, "projectUrl");
        assertNotNull(i18n, "i18n");
        assertNotNull(analysis, "analysis");

        final String notifyPrefix = isNotBlank(projectConfig.getNotify()) ? format("<!%s> ", projectConfig.getNotify()) : "";

        final QualityGate qualityGate = analysis.getQualityGate();
        final StringBuilder shortText = new StringBuilder();
        shortText.append(notifyPrefix);
        shortText.append(format("Project %s was analyzed", analysis.getProject().getName()));

        final Optional<Branch> branch = analysis.getBranch();
        if (branch.isPresent() && !branch.get().isMain() && this.includeBranch) {
            shortText.append(format(" for branch %s", branch.get().getName().orElse("")));
        }
        shortText.append(".");

        Payload payload = new Payload("Sonar report",
            new Section("Sonar Quality Report", shortText.toString()));

        if (qualityGate != null) {
            payload.getSection().getFacts().add(new Fact("Quality gate status",
                String.format("%s %s", statusSymbol.get(qualityGate.getStatus()), qualityGate.getStatus().toString())));
        }

        if (qualityGate != null) {
            payload.getSection().getFacts().addAll(buildFacts(qualityGate, projectConfig.isQgFailOnly()));
        }
        payload.getPotentialAction().add(new Action("See report", projectUrl));

        return payload;
    }

    private void assertNotNull(final Object object, final String argumentName) {
        if (object == null) {
            throw new IllegalArgumentException("[Assertion failed] - " + argumentName + " argument is required; it must not be null");
        }
    }

    private List<Fact> buildFacts(final QualityGate qualityGate, final boolean qgFailOnly) {
        return qualityGate.getConditions()
            .stream()
            .filter(condition -> !qgFailOnly || notOkNorNoValue(condition))
            .map(this::translate)
            .collect(Collectors.toList());
    }

    private boolean notOkNorNoValue(final QualityGate.Condition condition) {
        return !(QualityGate.EvaluationStatus.OK.equals(condition.getStatus())
            || QualityGate.EvaluationStatus.NO_VALUE.equals(condition.getStatus()));
    }

    private Fact translate(final QualityGate.Condition condition) {
        final String i18nKey = "metric." + condition.getMetricKey() + ".name";
        final String conditionName = i18n.message(Locale.ENGLISH, i18nKey, condition.getMetricKey());

        if (QualityGate.EvaluationStatus.NO_VALUE.equals(condition.getStatus())) {
            // No value for given metric
            return new Fact(conditionName, condition.getStatus().name());
        } else {
            final StringBuilder sb = new StringBuilder();
            appendValue(condition, sb);
            appendValuePostfix(condition, sb);
            if (condition.getErrorThreshold() != null) {
                sb.append(", error if ");
                appendValueOperatorPrefix(condition, sb);
                sb.append(condition.getErrorThreshold());
                appendValuePostfix(condition, sb);
            }
            return new Fact(conditionName,
                String.format("%s %s", evaluationStatusSymbol.get(condition.getStatus()), sb.toString()));
        }
    }

    private void appendValue(final QualityGate.Condition condition, final StringBuilder sb) {
        if ("".equals(condition.getValue())) {
            sb.append("-");
        } else {
            if (isPercentageCondition(condition)) {
                appendPercentageValue(condition.getValue(), sb);
            } else {
                sb.append(condition.getValue());
            }
        }
    }

    private void appendValuePostfix(final QualityGate.Condition condition, final StringBuilder sb) {
        if (isPercentageCondition(condition)) {
            sb.append("%");
        }
    }

    private void appendValueOperatorPrefix(final QualityGate.Condition condition, final StringBuilder sb) {
        switch (condition.getOperator()) {
            case EQUALS:
                sb.append("==");
                break;
            case NOT_EQUALS:
                sb.append("!=");
                break;
            case GREATER_THAN:
                sb.append(">");
                break;
            case LESS_THAN:
                sb.append("<");
                break;
            default:
                break;
        }
    }

    private boolean isPercentageCondition(final QualityGate.Condition condition) {
        switch (condition.getMetricKey()) {
            case CoreMetrics.NEW_COVERAGE_KEY:
            case CoreMetrics.NEW_SQALE_DEBT_RATIO_KEY:
                return true;
            default:
                break;
        }
        return false;
    }

    private void appendPercentageValue(final String s, final StringBuilder sb) {
        try {
            final Double d = Double.parseDouble(s);
            sb.append(percentageFormat.format(d));
        } catch (final NumberFormatException e) {
            LOG.error("Failed to parse [{}] into a Double due to [{}]", s, e.getMessage());
            sb.append(s);
        }
    }
}

