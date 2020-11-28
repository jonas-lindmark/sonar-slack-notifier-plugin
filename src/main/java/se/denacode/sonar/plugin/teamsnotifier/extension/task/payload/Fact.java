package se.denacode.sonar.plugin.teamsnotifier.extension.task.payload;

public class Fact {
    private final String name;
    private final String value;

    public Fact(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

}
