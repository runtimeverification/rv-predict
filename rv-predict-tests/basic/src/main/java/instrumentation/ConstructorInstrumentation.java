package instrumentation;

public class ConstructorInstrumentation extends ConstructorInstrumentationBaseClass {
    private int i1 = 7;
    private final int i2 = 8;
    final String s1 = new String();
    String s2 = new String(new String());
    public String s3 = new String(s2);
    private String s4;

    ConstructorInstrumentation() {
        super(new String(new String("Hello")));
        s4 = new String();
    }

    private ConstructorInstrumentation(String s) {
        this();
    }

    private ConstructorInstrumentation(String s1, String s2) {
        this(new String(s1));
    }

    private ConstructorInstrumentation(String s1, String s2, String s3) {
        this(new String(new String(s1)), new String(s2));
    }

    private ConstructorInstrumentation(ConstructorInstrumentation c1) {
        this(c1.toString());
    }

    private ConstructorInstrumentation(ConstructorInstrumentation c1, ConstructorInstrumentation c2) {
        this(new ConstructorInstrumentation(c1));
    }

    ConstructorInstrumentation(ConstructorInstrumentation c1, ConstructorInstrumentation c2, ConstructorInstrumentation c3) {
        this(new ConstructorInstrumentation(
                new ConstructorInstrumentation(c1),
                new ConstructorInstrumentation(
                        new ConstructorInstrumentation(c2), new ConstructorInstrumentation(c3))));
    }

    public static void main() {
    }
}
