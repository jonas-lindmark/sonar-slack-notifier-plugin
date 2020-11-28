package se.denacode.sonar.plugin.teamsnotifier.extension.task.payload;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class Action {
    @SerializedName("@context")
    private final String context = "http://schema.org";
    @SerializedName("@type")
    private final String type = "ViewAction";
    private final String name;
    private final List<String> target = new ArrayList<>();

    public Action(String name, String target) {
        this.name = name;
        this.target.add(target);
    }

    public String getContext() {
        return context;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public List<String> getTarget() {
        return target;
    }

}
