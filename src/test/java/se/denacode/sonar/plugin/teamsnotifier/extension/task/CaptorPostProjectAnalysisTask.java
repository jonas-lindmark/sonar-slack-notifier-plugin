package se.denacode.sonar.plugin.teamsnotifier.extension.task;

import org.sonar.api.ce.posttask.PostProjectAnalysisTask;

class CaptorPostProjectAnalysisTask implements PostProjectAnalysisTask {
    private ProjectAnalysis projectAnalysis;

    public String getDescription() {
        return "Capture the project informations";
    }

    @Override
    public void finished(final ProjectAnalysis analysis) {
        projectAnalysis = analysis;
    }

    public ProjectAnalysis getProjectAnalysis() {
        return this.projectAnalysis;
    }
}
