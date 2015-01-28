package rvpredict.config;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

public class Config {
    public static final Config instance = new Config();
    public static final String propFile = "rv.conf";

    public final Configuration commandLine = new Configuration();

    public final List<Pattern> excludeList = new LinkedList<>();
    public final List<Pattern> includeList = new LinkedList<>();

    public Config() {
        try {
            Properties properties = new Properties();

            properties.load(ClassLoader.getSystemClassLoader()// this.getClass().getClassLoader()
                    .getResourceAsStream(propFile));

            for (String exclude : properties.getProperty("rv.excludeList", "").split(",")) {
                if (exclude.isEmpty()) continue;
                excludeList.add(createRegEx(exclude.replace('.','/')));
            }
            for (String include : properties.getProperty("rv.includeList", "").split(",")) {
                if (include.isEmpty()) continue;
                includeList.add(createRegEx(include.replace('.','/')));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Creates a {@link java.util.regex.Pattern} from a String describing a package/class
     * using file pattern conventions ({@code *} standing for a sequence of characters)
     *
     * @param pattern the package/class description
     * @return A {@link java.util.regex.Pattern} which matches names specified by the given argument
     */
    public static Pattern createRegEx(String pattern) {
        String escapeChars[] = new String[] {".","$","["};
        for (String c : escapeChars) {
           pattern = pattern.replace(c, "\\"  + c);
        }
        return Pattern.compile(pattern.replace("*", ".*")+".*");
    }
}
