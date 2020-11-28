package se.denacode.sonar.plugin.teamsnotifier;

import static se.denacode.sonar.plugin.teamsnotifier.common.TeamsNotifierProp.CONFIG;
import static se.denacode.sonar.plugin.teamsnotifier.common.TeamsNotifierProp.ENABLED;
import static se.denacode.sonar.plugin.teamsnotifier.common.TeamsNotifierProp.HOOK;
import static se.denacode.sonar.plugin.teamsnotifier.common.TeamsNotifierProp.INCLUDE_BRANCH;
import static se.denacode.sonar.plugin.teamsnotifier.common.TeamsNotifierProp.PROXY_IP;
import static se.denacode.sonar.plugin.teamsnotifier.common.TeamsNotifierProp.PROXY_PORT;
import static se.denacode.sonar.plugin.teamsnotifier.common.TeamsNotifierProp.PROXY_PROTOCOL;
import se.denacode.sonar.plugin.teamsnotifier.extension.task.TeamsPostProjectAnalysisTask;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.sonar.api.Plugin;
import org.sonar.api.config.PropertyDefinition;

public class TeamsNotifierPluginTest {

    private final TeamsNotifierPlugin plugin = new TeamsNotifierPlugin();

    @Test
    public void define_expectedExtensionsAdded() {

        final Plugin.Context mockContext = mock(Plugin.Context.class);
        this.plugin.define(mockContext);
        final ArgumentCaptor<List> arg = ArgumentCaptor.forClass(List.class);
        verify(mockContext, times(1)).addExtensions(arg.capture());

        final List extensions = arg.getValue();
        Assert.assertEquals(8, extensions.size());
        Assert.assertEquals(HOOK.property(), ((PropertyDefinition) extensions.get(0)).key());
        Assert.assertEquals(PROXY_IP.property(), ((PropertyDefinition) extensions.get(1)).key());
        Assert.assertEquals(PROXY_PORT.property(), ((PropertyDefinition) extensions.get(2)).key());
        Assert.assertEquals(PROXY_PROTOCOL.property(), ((PropertyDefinition) extensions.get(3)).key());
        Assert.assertEquals(ENABLED.property(), ((PropertyDefinition) extensions.get(4)).key());
        Assert.assertEquals(INCLUDE_BRANCH.property(), ((PropertyDefinition) extensions.get(5)).key());
        Assert.assertEquals(CONFIG.property(), ((PropertyDefinition) extensions.get(6)).key());
        Assert.assertEquals(TeamsPostProjectAnalysisTask.class, extensions.get(7));

    }

    @SuppressWarnings("unchecked")
    @Test
    public void define_noDuplicateIndexes() {

        final Plugin.Context mockContext = mock(Plugin.Context.class);
        this.plugin.define(mockContext);
        final ArgumentCaptor<List> arg = ArgumentCaptor.forClass(List.class);
        verify(mockContext, times(1)).addExtensions(arg.capture());

        final List<Object> extensions = arg.getValue();

        final Set<Integer> indexes = extensions.stream().filter(PropertyDefinition.class::isInstance)
            .map(PropertyDefinition.class::cast).map(PropertyDefinition::index).
                collect(Collectors.toSet());
        Assert.assertEquals(7, indexes.size());

    }

}
