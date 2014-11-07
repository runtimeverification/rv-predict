package com.runtimeverification.rvpredict.engine.main;

import com.runtimeverification.rvpredict.IntegrationTest;
import com.runtimeverification.rvpredict.TestHelper;
import org.junit.experimental.categories.Category;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


/**
 * Base class for rv-predict system tests
 * @author TraianSF
 */
@RunWith(Parameterized.class)
@Category(IntegrationTest.class)
public class MainTest {
    private static String basePath = System.getProperty("rvPath");
    private static String separator = System.getProperty("file.separator");
    private static String examplesPath = basePath + separator + "examples";

    private static String getTestConfigPath() {
        String path = null;
        try {
            path = Paths.get(MainTest.class.getResource("/test.xml").toURI()).toAbsolutePath().toString();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return path;
    }

    private static String rvPredictJar = basePath + separator + "lib" + separator + "rv-predict.jar";
    private static String java = org.apache.tools.ant.util.JavaEnvUtils.getJreExecutable("java");
    private static List<String> rvArgList = Arrays.asList(new String[]{java, "-cp", rvPredictJar,
            "rvpredict.engine.main.Main"});
    private final TestHelper helper;
    private final String name;
    private final int numOfRuns;
    private final List<String> args;


    public MainTest(String name, String specPath, int numOfRuns, List<String> rvArguments, List<String> arguments) {
        this.name = name;
        this.numOfRuns = numOfRuns;
        helper = new TestHelper(specPath);
        args = new ArrayList<>(rvArgList);
        args.addAll(rvArguments);
        args.addAll(arguments);
    }

    /**
     * Builds the tests, then runs them.
     * Matches precomputed expected output files against the output generated by the tests.
     * @throws Exception
     */
    @Test
    public void testTest() throws Exception {
        String[] args = new String[this.args.size()];
        this.args.toArray(args);
        helper.testCommand("tests/" + name, numOfRuns, args);
    }

    // The method bellow creates the set of parameter instances to be used as seeds by
    // the test constructor.  Junit will run the testsuite once for each parameter instance.
    // This is documented in the Junit Parameterized tests page:
    // http://junit.sourceforge.net/javadoc/org/junit/runners/Parameterized.html
    @Parameterized.Parameters(name="{0}")
    public static Collection<Object[]> data() {
        Collection<Object[]> data = new ArrayList<Object[]>();
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(new File(getTestConfigPath()));
            NodeList tests = document.getElementsByTagName("test");
            for (int i = 0; i < tests.getLength(); i++) {
                Node node = tests.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element test = (Element) node;
                    String name = test.getAttribute("name");
                    List<String> arguments = new ArrayList<>();
                    List<String> rvarguments = new ArrayList<>();
                    int numOfRuns = getNumOfRuns(test);
                    processArguments(rvarguments, test.getElementsByTagName("rvarg"));
                    processArguments(arguments, test.getElementsByTagName("arg"));
                    data.add(new Object[]{ name, examplesPath, numOfRuns, rvarguments, arguments});
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    private static int getNumOfRuns(Element test) {
        NodeList nodeList = test.getElementsByTagName("runs");
        if (nodeList.getLength() == 0) {
            return 1;
        }
        
        assert nodeList.getLength() == 1;
        Node node = nodeList.item(0);
        return Integer.parseInt(((Element) node).getAttribute("value"));
    }

    private static void processArguments(List<String> arguments, NodeList args) {
        Node node;
        for (int j = 0; j < args.getLength(); j++) {
            node = args.item(j);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element arg = (Element) node;
                String key = arg.getAttribute("key");
                if (!key.isEmpty()) {
                    arguments.add(key);
                }
                arguments.add(arg.getAttribute("value"));
            }
        }
    }
}
