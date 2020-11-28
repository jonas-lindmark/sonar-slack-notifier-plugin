package se.denacode.sonar.plugin.teamsnotifier.common.component;

public class ProjectConfig {

    private final String projectHook;
    private final String projectKey;
    private final String notify;
    private final boolean qgFailOnly;

    public ProjectConfig(String projectHook, String projectKey, String notify, boolean qgFailOnly) {
        this.projectHook = projectHook;
        this.projectKey = projectKey;
        this.notify = notify;
        this.qgFailOnly = qgFailOnly;
    }

    public String getProjectHook() {
        return projectHook;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public String getNotify() {
        return notify;
    }

    public boolean isQgFailOnly() {
        return qgFailOnly;
    }
}
