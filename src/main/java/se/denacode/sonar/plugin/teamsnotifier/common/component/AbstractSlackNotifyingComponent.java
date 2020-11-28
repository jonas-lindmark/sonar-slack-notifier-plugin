package se.denacode.sonar.plugin.teamsnotifier.common.component;

import se.denacode.sonar.plugin.teamsnotifier.common.TeamsNotifierProp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;


public abstract class AbstractSlackNotifyingComponent {

    private static final Logger LOG = Loggers.get(AbstractSlackNotifyingComponent.class);

    private final Configuration configuration;
    private Map<String, ProjectConfig> projectConfigMap = Collections.emptyMap();

    public AbstractSlackNotifyingComponent(final Configuration configuration) {
        this.configuration = configuration;
        LOG.info("Constructor called, project config map constructed from general configuration");
    }

    /**
     * This method has to be called in the beginning of every actual plugin execution.
     * SonarQube seems to work in such a way that
     * <pre>
     * 1) the Settings object is constructor injected to this class.
     * 2) the values reflected by the Settings object reflect latest settings configured
     * 3) but the constructor of this class is called only once, and after that the class is never instantiated again
     * (the same instance is reused)
     * 4) thus when the instance is used to perform something, we must refresh the projectConfigMap when the
     * execution starts
     * </pre>
     */
    protected void refreshSettings() {
        LOG.info("Refreshing settings");
        this.refreshProjectConfigs();
    }

    private void refreshProjectConfigs() {
        LOG.info("Refreshing project configs");
        // final var oldValues =
        Set<ProjectConfig> oldValues = projectConfigMap.values().stream().
            map(ProjectConfigBuilder::cloneProjectConfig).collect(Collectors.toSet());
        projectConfigMap = buildProjectConfigByProjectKeyMap(configuration);
        // final Set newValues =
        Set<ProjectConfig> newValues = new HashSet<>(projectConfigMap.values());
        if (!oldValues.equals(newValues)) {
            LOG.info("Old configs [{}] --> new configs [{}]", oldValues, newValues);
        }
    }

    private static Map<String, ProjectConfig> buildProjectConfigByProjectKeyMap(final Configuration settings) {
        final Map<String, ProjectConfig> map = new HashMap<>();
        final String[] projectConfigIndexes = settings.getStringArray(TeamsNotifierProp.CONFIG.property());

        LOG.info("SlackNotifierProp.CONFIG=[{}]", (Object) projectConfigIndexes);
        for (final String projectConfigIndex : projectConfigIndexes) {
            final String projectKeyProperty = TeamsNotifierProp.CONFIG
                .property() + "." + projectConfigIndex + "." + TeamsNotifierProp.PROJECT_REGEXP
                .property();
            final Optional<String> projectKey = settings.get(projectKeyProperty);
            if (!projectKey.isPresent()) {
                throw MessageException.of(
                    "Slack notifier configuration is corrupted. At least one project specific parameter has no " +
                        "project key. " +
                        "Contact your administrator to update this configuration in the global administration section" +
                        " of SonarQube.");
            }
            final ProjectConfig value = new ProjectConfigBuilder()
                .withConfiguration(settings, TeamsNotifierProp.CONFIG.property() + "." + projectConfigIndex + ".")
                .build();
            LOG.info("Found project configuration [{}]", value);
            map.put(projectKey.get(), value);
        }
        return map;
    }

    protected String getIconUrl() {
        final Optional<String> icon = this.configuration.get(TeamsNotifierProp.ICON_URL.property());
        return icon.orElse(null);
    }

    protected boolean isPluginEnabled() {
        return this.configuration.getBoolean(TeamsNotifierProp.ENABLED.property())
            .orElseThrow(() -> new IllegalStateException("Enabled property not found"));
    }

    /**
     * @return value for INCLUDE_BRANCH property, defaults to false if for some reason not set.
     */
    protected boolean isBranchEnabled() {

        return this.configuration.getBoolean(TeamsNotifierProp.INCLUDE_BRANCH.property()).orElse(false);
    }

    /**
     * Returns the sonar server url, with a trailing /
     *
     * @return the sonar server URL
     */
    @SuppressWarnings("HardcodedFileSeparator")
    protected String getSonarServerUrl() {
        final Optional<String> urlOptional = this.configuration.get("sonar.core.serverBaseURL");
        if (!urlOptional.isPresent()) {
            return "http://pleaseDefineSonarQubeUrl/";
        }
        final String url = urlOptional.get();
        if (url.endsWith("/")) {
            return url;
        }
        return url + "/";
    }

    protected Optional<ProjectConfig> getProjectConfig(final String projectKey) {
        final List<ProjectConfig> projectConfigs = this.searchForProjectConfig(projectKey);
        // Not configured at all
        if (projectConfigs.isEmpty()) {
            LOG.info("Could not find config for project [{}] in [{}]", projectKey, this.projectConfigMap);

            LOG.info("Building the default project config.");
            return Optional.of(buildDefaultProjectConfig(projectKey));
        }

        if (projectConfigs.size() > 1) {
            LOG.warn("More than 1 project key was matched. Using first one: {}", projectConfigs.get(0).getProjectKey());
        }
        return Optional.of(projectConfigs.get(0));
    }

    @NotNull
    private List<ProjectConfig> searchForProjectConfig(final String projectKey) {
        List<ProjectConfig> list = new ArrayList<>();
        Map<String, ProjectConfig> stringProjectConfigMap = this.projectConfigMap;
        for (String s : this.projectConfigMap.keySet()) {
            //if (projectKey.matches(s)) {
            if (StringUtils.contains(s, projectKey)) {
                ProjectConfig projectConfig = stringProjectConfigMap.get(s);
                list.add(projectConfig);
            }
        }
        return list;
    }

    @NotNull
    private ProjectConfig buildDefaultProjectConfig(final String projectKey) {
        return new ProjectConfigBuilder().setProjectHook(this.getDefaultHook())
            .setProjectKeyOrRegExp(projectKey)
            .setQgFailOnly(false)
            .build();
    }

    protected String getDefaultHook() {
        final Optional<String> defaultHook = this.configuration.get(TeamsNotifierProp.HOOK.property());
        return defaultHook.orElse(null);
    }

    protected String logRelevantSettings() {
        final Map<String, String> pluginSettings = new HashMap<>();
        this.mapSetting(pluginSettings, TeamsNotifierProp.HOOK);
        this.mapSetting(pluginSettings, TeamsNotifierProp.PROXY_IP);
        this.mapSetting(pluginSettings, TeamsNotifierProp.PROXY_PORT);
        this.mapSetting(pluginSettings, TeamsNotifierProp.PROXY_PROTOCOL);
        this.mapSetting(pluginSettings, TeamsNotifierProp.ENABLED);
        this.mapSetting(pluginSettings, TeamsNotifierProp.CONFIG);
        this.mapSetting(pluginSettings, TeamsNotifierProp.INCLUDE_BRANCH);
        return pluginSettings + "; project specific channel config: " + this.projectConfigMap;
    }

    private void mapSetting(final Map<String, String> pluginSettings, final TeamsNotifierProp key) {
        pluginSettings.put(key.name(), this.configuration.get(key.property()).orElse(""));
    }

    protected boolean shouldSkipSendingNotification(final ProjectConfig projectConfig, final QualityGate qualityGate) {
        if (projectConfig.isQgFailOnly() && qualityGate != null && QualityGate.Status.OK.equals(
            qualityGate.getStatus())) {
            LOG.info("Project [{}] set up to send notification on failed Quality Gate, but was: {}",
                projectConfig.getProjectKey(), qualityGate.getStatus().name());
            return true;
        }
        return false;
    }
}
