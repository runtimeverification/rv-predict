package instrumentation;

public class ConstructorInstrumentationBaseClass {
    private int i1 = 7;
    private final int i2 = 8;
    final String s1 = new String();
    String s2 = new String(new String());
    public String s3 = new String(s2);
    public String s4;

    ConstructorInstrumentationBaseClass() {
        this("Hello");
    }

    ConstructorInstrumentationBaseClass(String s) {
        s4 = s;
    }
}
