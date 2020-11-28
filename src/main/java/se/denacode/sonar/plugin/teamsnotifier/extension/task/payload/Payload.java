package se.denacode.sonar.plugin.teamsnotifier.extension.task.payload;


import java.util.ArrayList;
import java.util.List;

public class Payload {
    private final String summary;
    private final String themeColor = "#00FF00";
    private final List<Section> sections = new ArrayList<>();
    private final List<Action> potentialAction = new ArrayList<>();

    public Payload(String summary, Section section) {
        this.summary = summary;
        this.sections.add(section);
    }

    public String getSummary() {
        return summary;
    }

    public String getThemeColor() {
        return themeColor;
    }

    public Section getSection() {
        return sections.get(0);
    }

    public List<Action> getPotentialAction() {
        return potentialAction;
    }

}



