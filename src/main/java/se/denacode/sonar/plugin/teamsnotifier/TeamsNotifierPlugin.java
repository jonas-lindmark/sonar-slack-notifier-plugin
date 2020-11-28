package se.denacode.sonar.plugin.teamsnotifier;

import se.denacode.sonar.plugin.teamsnotifier.extension.task.TeamsPostProjectAnalysisTask;
import java.util.ArrayList;
import java.util.List;
import org.sonar.api.Plugin;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyFieldDefinition;
import se.denacode.sonar.plugin.teamsnotifier.common.TeamsNotifierProp;

public class TeamsNotifierPlugin implements Plugin {

    private static final String CATEGORY = "Slack";
    private static final String SUBCATEGORY = "Sonar Slack Notifier";

    @Override
    public void define(final Context context) {
        final List<Object> extensions = new ArrayList<>();

        // The configurable properties
        this.addPluginPropertyDefinitions(extensions);

        // The actual plugin component(s)
        extensions.add(TeamsPostProjectAnalysisTask.class);

        context.addExtensions(extensions);
    }

    private void addPluginPropertyDefinitions(final List<Object> extensions) {
        extensions.add(PropertyDefinition.builder(TeamsNotifierProp.HOOK.property())
            .name("Officer web integration hook")
            .description("https://outlook.office.com/webhook/")
            .type(PropertyType.STRING)
            .category(CATEGORY)
            .subCategory(SUBCATEGORY)
            .index(0)
            .build());
        extensions.add(PropertyDefinition.builder(TeamsNotifierProp.PROXY_IP.property())
            .name("Proxy IP")
            .description("IP address of proxy server to use")
            .defaultValue("")
            .type(PropertyType.STRING)
            .category(CATEGORY)
            .subCategory(SUBCATEGORY)
            .index(1)
            .build());
        extensions.add(PropertyDefinition.builder(TeamsNotifierProp.PROXY_PORT.property())
            .name("Proxy port")
            .description("Port for proxy server")
            .defaultValue("8080")
            .type(PropertyType.INTEGER)
            .category(CATEGORY)
            .subCategory(SUBCATEGORY)
            .index(2)
            .build());
        extensions.add(PropertyDefinition.builder(TeamsNotifierProp.PROXY_PROTOCOL.property())
            .name("Proxy protocol")
            .description("Protocol to use to connect to proxy server")
            .defaultValue("HTTP")
            .type(PropertyType.SINGLE_SELECT_LIST)
            .options("DIRECT", "HTTP", "SOCKS")
            .category(CATEGORY)
            .subCategory(SUBCATEGORY)
            .index(3)
            .build());
        extensions.add(PropertyDefinition.builder(TeamsNotifierProp.ENABLED.property())
            .name("Plugin enabled")
            .description("Are Slack notifications enabled in general?")
            .defaultValue("false")
            .type(PropertyType.BOOLEAN)
            .category(CATEGORY)
            .subCategory(SUBCATEGORY)
            .index(4)
            .build());
        extensions.add(PropertyDefinition.builder(TeamsNotifierProp.INCLUDE_BRANCH.property())
            .name("Branch enabled")
            .description("Include branch name in messages?\nNB: Not supported with free version of SonarQube")
            .defaultValue("false")
            .type(PropertyType.BOOLEAN)
            .category(CATEGORY)
            .subCategory(SUBCATEGORY)
            .index(5)
            .build());
        extensions.add(
            PropertyDefinition.builder(TeamsNotifierProp.CONFIG.property())
                .name("Project specific configuration")
                .description("Project specific configuration: Specify notification only on failing Qualilty Gate.")
                .category(CATEGORY)
                .subCategory(SUBCATEGORY)
                .index(6)
                .fields(
                    PropertyFieldDefinition.build(TeamsNotifierProp.PROJECT_HOOK.property())
                        .name("Project Hook")
                        .description("https://api.slack.com/incoming-webhooks")
                        .type(PropertyType.STRING)
                        .build(),
                    PropertyFieldDefinition.build(TeamsNotifierProp.PROJECT_REGEXP.property())
                        .name("Project Key")
                        .description("Ex: com.koant.sonar.slack:sonar-slack-notifier-plugin, can use '*' wildcard at the end")
                        .description("Regex that will match the Project Key of the project. Ex: com\\..* will match all projects that start with 'com.'")
                        .type(PropertyType.STRING)
                        .build(),
                    PropertyFieldDefinition.build(TeamsNotifierProp.CHANNEL.property())
                        .name("Slack channel")
                        .description("Channel to send project specific messages to")
                        .type(PropertyType.STRING)
                        .build(),
                    PropertyFieldDefinition.build(TeamsNotifierProp.QG_FAIL_ONLY.property())
                        .name("Send on failed Quality Gate")
                        .description("Should notification be sent only if Quality Gate did not pass OK")
                        .type(PropertyType.BOOLEAN)
                        .build(),
                    PropertyFieldDefinition.build(TeamsNotifierProp.NOTIFY.property())
                        .name("Notify")
                        .description("add @ to someone before messages, for example @channel")
                        .type(PropertyType.STRING)
                        .build()
                )
                .build());
    }
}
