package se.denacode.sonar.plugin.teamsnotifier.common.component;

import se.denacode.sonar.plugin.teamsnotifier.common.TeamsNotifierProp;
import org.sonar.api.config.Configuration;

public class ProjectConfigBuilder {
    private String projectHook;
    private String projectKeyOrRegExp;
    private String notify;
    private boolean qgFailOnly;


    public ProjectConfigBuilder from(final ProjectConfig c) {
        projectHook = c.getProjectHook();
        projectKeyOrRegExp = c.getProjectKey();
        notify = c.getNotify();
        qgFailOnly = c.isQgFailOnly();
        return this;
    }

    public ProjectConfigBuilder setProjectHook(final String projectHook) {
        this.projectHook = projectHook;
        return this;
    }

    public ProjectConfigBuilder setProjectKeyOrRegExp(final String projectKeyOrRegExp) {
        this.projectKeyOrRegExp = projectKeyOrRegExp;
        return this;
    }

    public ProjectConfigBuilder setNotify(final String notify) {
        this.notify = notify;
        return this;
    }

    public ProjectConfigBuilder setQgFailOnly(final boolean qgFailOnly) {
        this.qgFailOnly = qgFailOnly;
        return this;
    }

    public ProjectConfig build() {
        return new ProjectConfig(
            this.projectHook,
            this.projectKeyOrRegExp,
            this.notify,
            this.qgFailOnly
        );
    }


    public ProjectConfigBuilder withConfiguration(final Configuration settings, final String configurationPrefix) {
        this.projectHook =
            settings.get(configurationPrefix + TeamsNotifierProp.PROJECT_HOOK.property()).orElse(null);
        this.projectKeyOrRegExp =
            settings.get(configurationPrefix + TeamsNotifierProp.PROJECT_REGEXP.property()).orElse("");
        this.notify = settings.get(configurationPrefix + TeamsNotifierProp.NOTIFY.property()).orElse("");
        this.qgFailOnly = settings.getBoolean(
            configurationPrefix + TeamsNotifierProp.QG_FAIL_ONLY.property()).orElse(true);
        return this;
    }

    public static ProjectConfig cloneProjectConfig(final ProjectConfig projectConfig) {
        return new ProjectConfigBuilder().from(projectConfig).build();
    }
}
