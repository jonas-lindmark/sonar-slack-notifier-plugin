package se.denacode.sonar.plugin.teamsnotifier.extension.task.payload;

import java.util.ArrayList;
import java.util.List;

public class Section {
    private final boolean markdown = true;
    private final List<Fact> facts = new ArrayList<>();
    private final String activityTitle;
    private final String activitySubtitle;


    public Section(String activityTitle, String activitySubtitle) {
        this.activityTitle = activityTitle;
        this.activitySubtitle = activitySubtitle;
    }

    public boolean isMarkdown() {
        return markdown;
    }

    public List<Fact> getFacts() {
        return facts;
    }

    public String getActivityTitle() {
        return activityTitle;
    }

    public String getActivitySubtitle() {
        return activitySubtitle;
    }

}
