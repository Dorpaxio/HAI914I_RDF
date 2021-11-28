package qengine.program;

public enum Option {
    QUERIES("queries", true),
    DATA("data", true),
    OUTPUT("output", true);

    private final boolean requireArgument;
    private final String name;

    Option(String name) {
        this(name, false);
    }

    Option(String name, boolean requireArgument) {
        this.name = name;
        this.requireArgument = requireArgument;
    }

    public boolean isRequireArgument() {
        return requireArgument;
    }

    public String getName() {
        return name;
    }
}
