//textPanel y = 630 px

//buttons y = 72 px
//rv predict y = 125 px
//total y = 341 px, need 289 px space

//images not the same size as button... ends up 240ish

package rvpredict;

import java.io.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.net.URL;
import java.net.URLClassLoader;

import java.awt.Component;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import java.awt.image.BufferedImage;
 
import javax.imageio.ImageIO;

import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.text.*;

import rvpredict.util.RootDirFinder;
import rvpredict.util.NoMainMethodException;

//might want to remove singleton object and make everything static
//it no longer really serves a purpose
public class GUIMain {
  static public final char BLACK   =  '\u0001';
  static public final char RED     =  '\u0002';
  static public final char GREEN   =  '\u0003';
  static public final char BLUE    =  '\u0004';
  static public final char YELLOW  =  '\u0005';
  static public final char GREY    =  '\u0006';
  
  static private long totalTime;
//  static private File tmpDir = new File("."); 
  static private String className = "";
  static private String absoluteFileName = "";
  static private HashMap<String, Long> modifiedMap = new HashMap<String, Long>();
  static private HashMap<String, File> tmpDirMap = new HashMap<String, File>();

  static private JFrame f;
  static private JTextPane textArea, testProgramTextArea;
  static private String rootPath = null;
  static private String commandArgs = "";
  static private String cpAppend = (System.getenv("CLASSPATH") == null)?
    "":System.getenv("CLASSPATH");
  static private String baseCP = System.getProperty("java.class.path"); 
  static private String predictionMode = "Data Races";
  static private String heapSize = "512m";
  static private GUIMain v = new GUIMain();
  static private JDialog predictionDialog;
  static final private JButton predictB = new JButton();
  static final private JButton testB = new JButton();
  static final private JButton killB = new JButton();
  static PrintStream stream, testStream;
  private boolean kill = false;
  static private boolean instrument = true;
  private boolean stopButtonPressed = true;

  static private JMenuBar menuBar = new JMenuBar();
  static private JMenuItem openItem,  exitItem, predictionItem, 
                           heapSizeItem, argsItem, classPathItem, 
                           aboutItem, helpItem;

  public static void main(String[] args){
    rootPath = args[0];
    createGUI(args[0]);
  }

  static private void createGUI(final String resourcePath){
    EventQueue.invokeLater(new Runnable() {
      public void run() {
        predictB.setIcon(new ImageIcon(resourcePath + "/lib/images/button-predict.png"));
        killB.setIcon(new ImageIcon(resourcePath + "/lib/images/button-stop.png"));
        testB.setIcon(new ImageIcon(resourcePath + "/lib/images/button-check.png"));

        predictB.setFont(new Font("Monospaced", Font.PLAIN, 12));
        killB.setFont(new Font("Monospaced", Font.PLAIN, 12));
        testB.setFont(new Font("Monospaced", Font.PLAIN, 12));

        // Create a JFrame, which is a Window with "decorations", i.e.
        // title, border and close-button
        f = new JFrame("RV Predict");
    
        // Set a simple Layout Manager that arranges the contained
        // Components
        f.setLayout(new BoxLayout(f.getContentPane(), BoxLayout.X_AXIS));
   
        // Set the default close operation for the window, or else the
        // program won't exit when clicking close button
        //  (The default is HIDE_ON_CLOSE, which just makes the window
        //  invisible, and thus doesn't exit the app)
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setJMenuBar(menuBar);

        JMenu fileMenu = new JMenu("File");
        createFileMenu(fileMenu);
        menuBar.add(fileMenu);

        JMenu settingsMenu = new JMenu("Settings");
        createSettingsMenu(settingsMenu);
        menuBar.add(settingsMenu);

        JMenu helpMenu = new JMenu("Help");
        createHelpMenu(helpMenu);
        menuBar.add(helpMenu);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        f.add(buttonPanel);
        f.add(Box.createRigidArea(new Dimension(6,0)));

        JPanel box = new JPanel();
        box.add(new JLabel(" "));
        buttonPanel.add(box);
        
        buttonPanel.add(testB);
        testB.setActionCommand("testB");
        testB.addActionListener(
          new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
              if ("testB".equals(e.getActionCommand())){ 
                 predictB.setEnabled(false); 
                 testB.setEnabled(false);
                 killB.setEnabled(true);
                 disableSettings();
                 (new Thread(){
                   @Override 
                   public void run(){
                     v.kill = false;
                     v.stopButtonPressed = false;
                     v.runTest();
                     predictB.setEnabled(true); 
                     testB.setEnabled(true);
                     killB.setEnabled(false);
                     enableSettings();
                   } 
                 }).start();
              } 
            }
          });
        testB.setEnabled(true);

        buttonPanel.add(predictB);
        predictB.setActionCommand("predict");
        predictB.addActionListener(
          new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
              if ("predict".equals(e.getActionCommand())){ 
                 if (!("Data Races".equals(predictionMode))){
                    String stars = mkXStr(predictionMode.length(), "*");
                    System.out.println("********************************" + stars);
                    System.out.println("* " + RED + "Sorry, " + predictionMode + " is not yet supported *");
                    System.out.println("********************************" + stars);
                    return;
                 }
                 predictB.setEnabled(false); 
                 testB.setEnabled(false);
                 killB.setEnabled(true);
                 disableSettings();
                 (new Thread(){
                   @Override 
                   public void run(){
                     v.kill = false;
                     v.stopButtonPressed = false;
                     v.runRVPredict();
                     predictB.setEnabled(true); 
                     testB.setEnabled(true);
                     killB.setEnabled(false);
                     enableSettings();
                   } 
                 }).start();
              } 
            }
          });
        predictB.setEnabled(true);
    
        buttonPanel.add(killB);
        killB.setActionCommand("killB");
        killB.addActionListener(
          new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
              if ("killB".equals(e.getActionCommand())){ 
                 killAction();
              } 
            }
          });
        killB.setEnabled(false);

        try{
          BufferedImage predictImage = ImageIO.read(new File(resourcePath + "/lib/images/rv-predict-logo.png"));
          JLabel picLabel = new JLabel(new ImageIcon(predictImage));
        //  buttonPanel.add(Box.createRigidArea(new Dimension(0, 240)));
          JPanel j = new JPanel();
          j.setMinimumSize(new Dimension(10,10));
          buttonPanel.add(j);
          buttonPanel.add(picLabel);
        } catch (IOException e){
          e.printStackTrace();
        }

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        f.add(textPanel);

        JPanel clearPanel = new JPanel();
        clearPanel.setLayout(new BoxLayout(clearPanel, BoxLayout.X_AXIS));
        clearPanel.add(new JLabel("RV Predict output window"));
        textPanel.add(clearPanel);
        JPanel spacer = new JPanel();
        spacer.setMinimumSize(new Dimension(1,1));
        clearPanel.add(spacer);
        JButton saveLog = new JButton("Save Log");
        clearPanel.add(saveLog);
        saveLog.setActionCommand("saveLog");
        saveLog.addActionListener(
        new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e){
            if ("saveLog".equals(e.getActionCommand())){ 
               runLogFileChooser();
            } 
          }
        });
        JButton clear = new JButton("Clear");
        clear.setActionCommand("clear");
        clear.addActionListener(
          new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
              if ("clear".equals(e.getActionCommand())){ 
                textArea.setText("");
              } 
            }
          });
        clear.setEnabled(true);
        clearPanel.add(clear);
        clearPanel.setMinimumSize(new Dimension(1, clearPanel.getPreferredSize().height));
        clearPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, clearPanel.getPreferredSize().height));
       
        //set spacing over the three control buttons
        box.setMinimumSize(box.getPreferredSize());
        box.setMaximumSize(box.getPreferredSize());

        textArea = new JTextPane();
        textArea.setEditable(false);

        addStyles(textArea);

        textArea.setText("Welcome to the Beta version of RV Predict.\n"
                        + "Input an application to test using the Open item in the File menu.\n"  
                        + "See the Help menu for help.\n\n"
                        +  "Please keep in mind that this is a beta program with many performance\n"
                + "improvements in the works that will improve performance by orders of magnitude.\n\n"  
                       // + "Also, we currently only support the main\n"
                       // + "Thread constructs of java.  Several components of java.util.concurrent\n"
                       // + "are not implemented in terms of java's main Thread capabilities and\n"
                       // + "may result in false races (for instance ExecutorService.invokeAll does\n"
                       // + "not properly introduce a sequence point and will race with the following\n"
                       // + "lines of code in the Thread from which it is invoked).\n\n"
                        );

        JPanel noWrap = new JPanel();
        noWrap.setLayout(new BorderLayout());
        noWrap.add(textArea);
        JScrollPane scrollPane = new JScrollPane(noWrap);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(1050,450));
        scrollPane.setMinimumSize(new Dimension(1050,450));

       // textPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        textPanel.add(scrollPane);
//        textPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        clearPanel = new JPanel();
        clearPanel.setLayout(new BoxLayout(clearPanel, BoxLayout.X_AXIS));
        textPanel.add(clearPanel);
        clearPanel.add(new JLabel("Test Program output window"));
        spacer = new JPanel();
        spacer.setMinimumSize(new Dimension(1,1));
        clearPanel.add(spacer);
        clear = new JButton("Clear");
        clear.setActionCommand("clear");
        clear.addActionListener(
          new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
              if ("clear".equals(e.getActionCommand())){ 
                testProgramTextArea.setText("");
              } 
            }
          });
        clear.setEnabled(true);
        clearPanel.add(clear);
        clearPanel.setMinimumSize(new Dimension(1, clearPanel.getPreferredSize().height));
        clearPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, clearPanel.getPreferredSize().height));

        testProgramTextArea = new JTextPane();

        testProgramTextArea.setEditable(false);

        addStyles(testProgramTextArea);
        
 
        noWrap = new JPanel();
        noWrap.setLayout(new BorderLayout());
        noWrap.add(testProgramTextArea);
        scrollPane = new JScrollPane(noWrap);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(1050,250));
        scrollPane.setMinimumSize(new Dimension(1050,250));
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE,250));
        
        textPanel.add(scrollPane);
        textPanel.add(Box.createRigidArea(new Dimension(0, 10)));


        f.add(textPanel);
 
        f.add(Box.createRigidArea(new Dimension(10, 0)));
        // "Pack" the window, making it "just big enough".
        f.pack();
        f.setMinimumSize(f.getPreferredSize());

    
        stream = new PrintStream(new JTextPaneOutputStream(textArea), true); 
        testStream = new PrintStream(new JTextPaneOutputStream(testProgramTextArea), true); 

        System.setOut(stream); 
       // System.setOut(new PrintStream(new JTextAreaOutputStream(textArea), true));
        // Set the visibility as true, thereby displaying it
        f.setVisible(true);
      }
    });
  }

  private static void addStyles(JTextPane textArea){
    Style style = textArea.addStyle("black", null);
    StyleConstants.setForeground(style, Color.black);  
    StyleConstants.setFontFamily(style, "Monospaced"); 
    style = textArea.addStyle("blue", null);
    StyleConstants.setForeground(style, Color.blue);  
    StyleConstants.setFontFamily(style, "Monospaced");
    style = textArea.addStyle("green", null);
    StyleConstants.setBold(style, true);  
    StyleConstants.setForeground(style, new Color(0f,.4f,0f));  
    StyleConstants.setFontFamily(style, "Monospaced");
    style = textArea.addStyle("red", null);
    StyleConstants.setForeground(style, Color.red);  
    StyleConstants.setBold(style, true);  
    StyleConstants.setFontFamily(style, "Monospaced");
    style = textArea.addStyle("yellow", null);
    StyleConstants.setForeground(style, new Color(.4f,.4f,0f));  
    StyleConstants.setFontFamily(style, "Monospaced");
    style = textArea.addStyle("grey", null);
    StyleConstants.setBackground(style, new Color(.8f,.8f,.8f));  
    StyleConstants.setFontFamily(style, "Monospaced");
  }

  private static void killAction(){
    v.kill = true;
    v.stopButtonPressed = true;
    killB.setEnabled(false);
    testB.setEnabled(true);
    predictB.setEnabled(true);
    enableSettings();
    cleanUp();
  }

  private static void enableSettings(){
    predictionItem.setEnabled(true);
    heapSizeItem.setEnabled(true);
    argsItem.setEnabled(true);
    classPathItem.setEnabled(true);
  }

  private static void disableSettings(){
    predictionItem.setEnabled(false);
    heapSizeItem.setEnabled(false);
    argsItem.setEnabled(false);
    classPathItem.setEnabled(false);
  }

  public static void createFileMenu(JMenu fileMenu){
    openItem = fileMenu.add("Open"); 
    fileMenu.addSeparator();
    exitItem = fileMenu.add("Exit");

    openItem.setActionCommand("fileChoose");
    openItem.addActionListener(
      new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e){
          if ("fileChoose".equals(e.getActionCommand())){ 
             runFileChooser();
          } 
        }
      });
    openItem.setEnabled(true);

    exitItem.setActionCommand("exit");
    exitItem.addActionListener(
      new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e){
          if ("exit".equals(e.getActionCommand())){ 
             if(JOptionPane.showConfirmDialog(null, "Are you sure you wish to exit?", "Exit?", 
                                              JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
             System.exit(0);
          } 
        }
      });
    exitItem.setEnabled(true);
  }

  private static void createSettingsMenu(JMenu settingsMenu){
    predictionItem = settingsMenu.add("Prediction Algorithm");
    settingsMenu.addSeparator();
    classPathItem = settingsMenu.add("Classpath");
    heapSizeItem = settingsMenu.add("Heap Size");
    argsItem = settingsMenu.add("Program Arguments"); 
    
    predictionItem.setActionCommand("prediction");
    predictionItem.addActionListener(
      new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e){
          if ("prediction".equals(e.getActionCommand())){ 
        /*     String newPredictionMode = (String) JOptionPane.showInputDialog(null,
                                            "Choose a prediction algorithm", 
                                            "Prediction Algorithm",
                                            JOptionPane.PLAIN_MESSAGE,
                                            null,
                                            new Object[] {"Data Races", 
                                                          "Deadlocks (coming soon)", 
                                                          "Atomicity Violations (coming soon)",
                                                          "Generic Property (coming soon)"},
                                            "Data Races"
                                      );
            predictionMode = (newPredictionMode == null) ? predictionMode : newPredictionMode;
*/
             predictionDialog = new JDialog(f, "Prediction Algorithm", true);
             //predictionDialog.setLayout(new BoxLayout(predictionDialog, BoxLayout.Y_AXIS));
             predictionDialog.setLayout(new BoxLayout(predictionDialog.getContentPane(), BoxLayout.Y_AXIS));

             JRadioButton dataRaceButton = new JRadioButton("Data Race Detection", true);
             dataRaceButton.setActionCommand("dataRaceButton");
             dataRaceButton.addActionListener(
               new ActionListener() {
                 @Override
                 public void actionPerformed(ActionEvent e){
                   if ("dataRaceButton".equals(e.getActionCommand())){ 
                      predictionMode = "Data Races";
                   } 
                 }
               });

             JRadioButton deadlockButton = new JRadioButton("Deadlock Detection (coming soon)");
             JRadioButton atomButton = new JRadioButton("Atomicity Violation Detection (coming soon)");
             JRadioButton genericButton = new JRadioButton("Generic Property Detection (coming soon)");

             deadlockButton.setEnabled(false);
             atomButton.setEnabled(false);
             genericButton.setEnabled(false);

             ButtonGroup group = new ButtonGroup();
             group.add(dataRaceButton);
             group.add(deadlockButton);
             group.add(atomButton);
             group.add(genericButton);

             predictionDialog.add(dataRaceButton);
             predictionDialog.add(deadlockButton);
             predictionDialog.add(atomButton);
             predictionDialog.add(genericButton);

             JButton okButton = new JButton("Ok");
             okButton.setActionCommand("ok");
             okButton.addActionListener(
               new ActionListener() {
                 @Override
                 public void actionPerformed(ActionEvent e){
                   if ("ok".equals(e.getActionCommand())){ 
                      predictionDialog.dispose();
                   } 
                 }
               });

             predictionDialog.add(okButton);
             predictionDialog.pack();
             predictionDialog.setLocationRelativeTo(null);
             predictionDialog.setVisible(true);
          } 
        }
      });

    heapSizeItem.setActionCommand("heapSize");
    heapSizeItem.addActionListener(
      new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e){
          if ("heapSize".equals(e.getActionCommand())){ 
             String newHeapSize = JOptionPane.showInputDialog("Please enter the heap size you wish to use for prediction", 
                                                       heapSize);
            if(newHeapSize != null) {
              heapSize = newHeapSize;
              printInfo();
            }
          } 
        }
      });
 
    argsItem.setActionCommand("args");
    argsItem.addActionListener(
      new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e){
          if ("args".equals(e.getActionCommand())){ 
             String newCommandArgs = JOptionPane.showInputDialog("Please enter the arguments you wish passed to your program (excluding class path)", 
                                                       commandArgs);
            if(newCommandArgs != null) {
              commandArgs = newCommandArgs;
              printInfo();
            }
          } 
        }
      });

    classPathItem.setActionCommand("CP");
    classPathItem.addActionListener(
      new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e){
          if ("CP".equals(e.getActionCommand())){ 
             String newCpAppend = JOptionPane.showInputDialog("Please enter any additional class path entries necessary to run your program.  Initially this is set to your CLASSPATH system variable.", 
                                                       cpAppend);
             
             if(newCpAppend != null){
               cpAppend = newCpAppend;
               printInfo();
             }
          } 
        }
      });
  }

  private static void createHelpMenu(JMenu helpMenu){
    helpItem = helpMenu.add("Usage");
    helpItem.setActionCommand("helpB");
    helpItem.addActionListener(
      new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e){
          if ("helpB".equals(e.getActionCommand())){ 
            JDialog help = new JDialog((java.awt.Frame)null,"RV-Predict Help");
            //here we use a JTextPane instead of JTextArea because it 
            //fits the text exactly, and because it can format as html
            JTextPane helpTxt = new JTextPane();
            helpTxt.setContentType("text/html");
            helpTxt.setText( 
  "&nbsp;&nbsp;Welcome to RV Predict<br /><br />"
+ "&nbsp;&nbsp; RV Predict has three buttons:<br />"
+ "<ul><li> Test - Tests the currently selected program without instrumentation.  This is useful for checking that the classpath and arguments are correct.</li>"
+ "<li> Predict - Runs the selected prediction algorithm on the current program. </li>"
+ "<li> Stop - Stops the current test run or prediction run. </li></ul><br />"
+ "&nbsp;&nbsp; RV Predict has three menus:<br />"
+ "<ul><li>File</li><li>Settings</li><li>Help</li></ul>"
+ "&nbsp;&nbsp; The File menu has four options:<br />"
+ "<ul><li> Open - This allows for the selection of a class file to test.</li>"
+ "<li> Save Log - This allows the current prediction session (the entire contents of the window) to be saved to a selected file.</li>"
+ "<li> Clear Dialog Window - This clears the current session dialog window.</li>"
+ "<li> Exit - exits the RV Predict </li></ul>" 
+ "&nbsp;&nbsp; The Settings menu has four options:<br />"
+ "<ul><li>Prediction Algorithm - This allows for deciding between prediction algorithms.  Currently, only Data Race Prediction is enabled.</li>"
+ "<li>Heap Size - This allows for specifying the Heap Size to use for prediction.</li>"
+ "<li>Program Arguments - This allows for specifying arguments needed for the program under test.  Double quotes must be put around "
+ "arguments that contain spaces.&nbsp;&nbsp;</li>"
+ "<li>Classs Path - This allows for adding classpath entries to the program under test.</li></ul>"
+ "&nbsp;&nbsp; The Help menu has two options:<br />"
+ "<ul><li>Usage - This opens the current page.</li>"
+ "<li>About - This contains general information about RV Predict.</li></ul>"

            );
           helpTxt.setEditable(false);
           help.add(helpTxt);
           help.pack();
           help.setLocationRelativeTo(null);
           help.setVisible(true);
          } 
        }
      });

    aboutItem = helpMenu.add("About");
    aboutItem.setActionCommand("about");
    aboutItem.addActionListener(
      new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e){
          if ("about".equals(e.getActionCommand())){
            JDialog about = new JDialog((java.awt.Frame)null, "RV-Predict About");
            JTextPane aboutTxt = new JTextPane();
            aboutTxt.setContentType("text/html");
            aboutTxt.setText(
   "&nbsp;&nbsp;RV Predict is a beta tool for evaluation purposes only.  Currently the only available prediction algorithm is "
 + "for data races.&nbsp;&nbsp;<br />"
 + "&nbsp;&nbsp;RV Predict works by stepping through five steps:"
 + "<ul><li> Instrumentation - the program under test is instrumented to emit important events. </li>"
 + "<li> Running the Test Program - the test program is then ran to produce traces.  Traces are compressed and emitted in reverse. </li>"
 + "<li> Trace Slicing - Traces are sliced so that only relevant events are included.  This produces a more relaxed causal model that " 
 + "is able to find more violations.&nbsp;&nbsp<br />"
 + "Slicing must traverse events in reverse, which is why events are emitted in reverse.&nbsp;&nbsp;</li>"
 + "<li> Vector Clocking - the trace slices are vector clocked so that a causal ordering may be applied to them.  "
 + "This process proceeds in reverse.</li>"
 + "<li> Prediction - the various prediction algorithms (currently only data race detection) are performed on the reversed, vector clocked, "
 + "trace</li></ul>" 
            ); 
            aboutTxt.setEditable(false);
            about.add(aboutTxt);
            about.pack();
            about.setLocationRelativeTo(null);
            about.setVisible(true);
          }
        }
      }
    );  
  }

  private static void printInfo(){
    int length = Math.max(heapSize.length() - 8, Math.max(cpAppend.length() - 8, commandArgs.length()));
    String stars = mkXStr(length, "*");
  
    System.setOut(stream); 
    System.out.println("************************" + stars);
    System.out.println("* Classpath - " + cpAppend + mkXStr((8 + length) - cpAppend.length(), " ") + " *");
    System.out.println("* Heap Size - " + heapSize + mkXStr((8 + length) - heapSize.length(), " ") + " *");
    System.out.println("* Program Arguments - " + commandArgs + mkXStr(length - commandArgs.length(), " ") + " *");
    System.out.println("************************" + stars);
  }

  private static void runLogFileChooser(){
    JFileChooser chooser = new JFileChooser();
    f.add(chooser);

    if(rootPath != null) chooser.setCurrentDirectory(new File(rootPath));

    int retVal = chooser.showSaveDialog(f);
    if(retVal == JFileChooser.APPROVE_OPTION){
      File file = chooser.getSelectedFile();
      if(!file.exists()){
        if(JOptionPane.showConfirmDialog(null, "File does not exist, do you wish to create a new file?", "File Does Not Exist", 
                                                  JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
          try{
            file.createNewFile();
          } catch (IOException e){
            JOptionPane.showMessageDialog(null, "Cannot create file", "Cannot Create File", JOptionPane.WARNING_MESSAGE);
            return;
          }     
        }
      }
      if(!file.canWrite()){
        JOptionPane.showMessageDialog(null, "Cannot write file", "Cannot Write File", JOptionPane.WARNING_MESSAGE);
        return;
      } 
      try{
        FileOutputStream fos = new FileOutputStream(file); 
        PrintStream ps = new PrintStream(fos);
        ps.print(textArea.getText());
        ps.close();
      } catch (Exception e){
        JOptionPane.showMessageDialog(null, "Cannot write file", "Cannot Write File", JOptionPane.WARNING_MESSAGE);
        e.printStackTrace();
        return;
      }
    }
  }

  private static void runFileChooser(){
    JFileChooser chooser = new JFileChooser();
    FileNameExtensionFilter filter = new FileNameExtensionFilter("Java class files or jars", "class", "jar");
    chooser.setFileFilter(filter);
    if(rootPath != null) chooser.setCurrentDirectory(new File(rootPath));
    f.add(chooser);

    int retVal = chooser.showOpenDialog(f);
    if(retVal == JFileChooser.APPROVE_OPTION){
      File file = chooser.getSelectedFile();
      try{
        absoluteFileName = file.getCanonicalPath();
      } catch (java.io.IOException e){
        System.out.println(RED + "Chosen file cannot be accessed.");
        return;
      }
      if(file.getName().endsWith("jar")){
        handleJar(file);
      }
      else {
        handleClass(file);
      }
      System.out.println("");
      predictB.setEnabled(true);
      testB.setEnabled(true);
    }
  }

  private static void handleJar(File file){
    cpAppend = file.getAbsolutePath() + File.pathSeparator + cpAppend;
    rootPath = file.getParent();
    try{
      JarFile jf = new JarFile(file);
      Manifest manifest = jf.getManifest();
      className = manifest.getMainAttributes().getValue("Main-Class");
 
      className = className.replaceAll("[\\/]",".");
      //System.out.println(RED + className);

      long timestamp = file.lastModified();
      if(modifiedMap.containsKey(absoluteFileName)){
        if(timestamp != modifiedMap.get(absoluteFileName)){
          instrument = true;
          modifiedMap.put(absoluteFileName, timestamp);
           System.out.println(GREEN 
          + "  uninstrumented jar file selected, clearing program arguments (but leaving classpath).");
        } 
        else {
          instrument = false;
        }
      }
      else {
        instrument = true;
        modifiedMap.put(absoluteFileName, timestamp);
      }

      String stars = mkXStr(rootPath.length(), "*");
      System.out.println("*********************************" + stars);
      System.out.println("* Current main application class file: " + className + " *");
      System.out.println("*********************************" + stars);

    } catch (IOException e){
        System.out.println(RED + "  Could not load specified jar file.");
        System.out.println(RED + "    Are you sure that it is a jar file and in the proper directory?");
        return;
    }
  }
  
  private static void handleClass(File file){

      RootDirFinder r = new RootDirFinder(file); 
      try{
        rootPath = r.getRootDir();
        className = r.getClassName();
        if(!className.equals("")){

           long timestamp = file.lastModified();
           if(modifiedMap.containsKey(absoluteFileName)){
             if(timestamp != modifiedMap.get(absoluteFileName)){
               instrument = true;
               modifiedMap.put(absoluteFileName, timestamp);
               System.out.println(GREEN 
          + "  uninstrumented class file selected, clearing program arguments (but leaving classpath).");
             } 
             else {
               instrument = false;
             }
          }
          else {
            instrument = true;
            modifiedMap.put(absoluteFileName, timestamp);
          }
          commandArgs = ""; 
        }
      } catch (NoMainMethodException e) {
        System.out.println(RED + "  " + file.getName() + " does not contain a main method.");
        rootPath = file.getParent();
        return;
      } catch (IOException e){
        System.out.println(RED + "  Could not load specified class file.");
        System.out.println(RED + "    Are you sure that it is a class file and in the proper directory?");
        rootPath = file.getParent();
        return;
      } 
      String stars;
      String spaces;
      if(rootPath.length() > className.length()){
        stars = mkXStr(rootPath.length(), "*");
        spaces = mkXStr(rootPath.length() - className.length(), " ");
        System.out.println("*****************************************" + stars);
        System.out.println("* Current application root directory:  " + rootPath + " *"); 
        System.out.println("* Current main application class file: " + className + spaces + " *");
        System.out.println("*****************************************" + stars);
        printInfo();
      } else {
        stars = mkXStr(className.length(), "*");
        spaces = mkXStr(className.length() - rootPath.length(), " ");
        System.out.println("*****************************************" + stars);
        System.out.println("* Current application root directory:  " + rootPath + spaces + " *"); 
        System.out.println("* Current main application class file: " + className + " *");
        System.out.println("*****************************************" + stars);
        printInfo();
      }

  }

  private static String mkXStr(int i, String s){
    String stars = "";
    for(; i > 0; --i) stars += s;
    return stars;
  }

  private static String mkStars(float t){
    String s = String.valueOf(t);
    String stars = "";
    for(int i = 0; i < s.length(); ++i) stars += "*";
    return stars;
  }

  private void printStop(){
    System.setOut(stream); 
    System.out.println("***********");
    System.out.println("* Stopped *");
    System.out.println("***********");
  }

  private void runRVPredict(){
    if(className.equals("")){
      System.out.println(RED + "  No class selected, terminating prediction.");
      return;
    }
    System.out.println("*******************************");
    System.out.println("* Starting New Prediction Run *");
    System.out.println("*******************************");
    totalTime = System.currentTimeMillis();
    File f = new File(absoluteFileName);
    long timestamp = modifiedMap.get(absoluteFileName);
    if(timestamp != f.lastModified()){
      System.out.println("************************************************************");
      System.out.println("* Selected file has been modified since previous selection *");
      System.out.println("************************************************************");
      instrument = true;
    }
    if(instrument){
      instrument();
      if(v.kill){ 
        printStop();
        return;
      }
      instrument = false;
    }
    else {
      System.out.println(
          "**************************************************************************************");
      System.out.println( 
          "* Selected class file has not been modified since last time it was instrumented      *\n"
          +"* and will not be instrumented.  If you wish to reinstrument this program            *\n"
          +"* change the main class files modification time (touch on POSIX compliant systems)   *"
          );
      System.out.println(
          "**************************************************************************************");
    }
    //setup additional args, will eventually be specified through GUI
    runProgram();
    if(v.kill) {
      printStop();
      return;
    }
    predict(); 
    if(v.kill) printStop();
    cleanUp();
  }

  private File mkTempDir(){
   File f = null;
   File dir = null;
   try{
     f = File.createTempFile("rvpredict", "");
     //windows may not allow immediate deletion of f.  To be safe
     //we just append "d"
     String dirName = f.getAbsolutePath() + "d";
     dir = new File(dirName); 
     dir.mkdir();
     f.delete();
   }
   catch(IOException ioe){
    ioe.printStackTrace();
    System.exit(1);
   }
   return dir;
  }

  private static class ProcessOutputReader extends Thread {
    static final Pattern filter
      = Pattern.compile("INFO: Creating new CacheManager singleton" +
                       "|com.whirlycott.cache" +
                       "|INFO: Size: [0-9]*; Questions: [0-9,]*;");

    BufferedReader reader;
    boolean error;
    char color;

    ProcessOutputReader(BufferedReader reader, char color, boolean error){
      this.reader = reader;
      this.error = error;
      this.color = color;
    }

    @Override
    public void run(){
      String line;
      try{ 
        if(error)
            while ((line = reader.readLine()) != null) {
               Matcher m = filter.matcher(line);
               if(!m.find())  synchronized(System.out) { System.out.println(color + line); }
            }
        else 
            while ((line = reader.readLine()) != null) synchronized(System.out) { System.out.println(color + line); }
      }
      catch(Exception e){
       // This exception actually happens occasionally when we have to destroy the process, so we just ignore it
       // e.printStackTrace();
      }
    }
  }

  private static class SootErrorReader extends Thread {
    BufferedReader reader;

    static final Pattern filter
      = Pattern.compile("INFO: Creating new CacheManager singleton" +
                       "|com.whirlycott.cache" +
                       "|INFO: Size: [0-9]*; Questions: [0-9,]*;" +
                       "|^\\s+");

     SootErrorReader(BufferedReader reader){
       this.reader = reader;
     }
     
    @Override
    public void run(){
      String line;
      try{ 
        while ((line = reader.readLine()) != null) {
         // System.out.println(RED + "!!!!!!!!!!!" + line);
          Matcher m = filter.matcher(line);
          if(!m.find()) {
            //java.lang.RuntimeException: couldn't find class: jgfutil.JGFInstrumentor (is your soot-class-path set properly?)
            line = line.replaceAll("java.lang.RuntimeException:", "");
            line = line.replaceAll("is your soot-class-path set properly?", "did you use the correct classpath?");
            synchronized(System.out) { System.out.println(RED + line); }
          }
        }
      }
      catch(Exception e){
        e.printStackTrace();
        System.exit(1);
      }
    }
  }

  //Because we want to be able to kill the prediction process we
  //spin on the outputstreams still being alive and destroy the process
  //if the kill signal is indicated
  private void readExternalProcess(Process p, char outColor, char errorColor) throws InterruptedException {
    BufferedReader output = new BufferedReader(new InputStreamReader(p.getInputStream()));
    BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
    ProcessOutputReader outputR = new ProcessOutputReader(output, outColor, false);
    ProcessOutputReader errorR = new ProcessOutputReader(error, errorColor, true);
    outputR.start();
    errorR.start();
    while(outputR.isAlive() || errorR.isAlive()){
      if(v.kill){
        p.destroy();
      } 
    }
    outputR.join();
    errorR.join();
    p.waitFor(); //this really shouldn't be necessary.  Not sure how we ever get here with the process not exited
                 //but jvm claims we are occasionally
  }

  //This only difference from readExternalProcess on the error reading.
  //probably refactor
  private void readSootProcess(Process p) throws InterruptedException {
    BufferedReader output = new BufferedReader(new InputStreamReader(p.getInputStream()));
    BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
    ProcessOutputReader outputR = new ProcessOutputReader(output, BLACK, false);
    SootErrorReader errorR = new SootErrorReader(error);
    outputR.start();
    errorR.start();
    while(outputR.isAlive() || errorR.isAlive()){
      if(v.kill){
        p.destroy();
      } 
    }
    outputR.join();
    errorR.join();
    p.waitFor(); //this really shouldn't be necessary.  Not sure how we ever get here with the process not exited
                 //but jvm claims we are occasionally
  }


  private void instrument(){
    File tmpDir = mkTempDir();
    tmpDirMap.put(absoluteFileName, tmpDir);
    String cp = rootPath + File.pathSeparator + cpAppend + File.pathSeparator + baseCP;

    System.out.println("*********************************************");
    System.out.println("* Uninstrumented class found, instrumenting *");
    System.out.println("*********************************************");
    System.out.println(GREEN + "  Program will be executed in " + tmpDir.getAbsolutePath() + ".\n");
    String[] cmd = {"java", "-cp", cp, "-Xmx" + heapSize, "rvpredict.instrumentation.Main", 
                            "-app", className, "-d", tmpDir.getAbsolutePath(), 
                            "-validate", "-x", "com.google.protobuf", "rvpredict", 
                            "com.ning.compress.lzf", "jdbm", "java"};
    try{
      Process p = Runtime.getRuntime().exec(cmd);
      readSootProcess(p);
      if(!((p.exitValue() == 0) || v.stopButtonPressed)) killAction();
      if(v.kill) return;
    } catch(Exception e){
       e.printStackTrace();
    }
  }

  private String[] createCommand(String cp){
    ArrayList<String> commandArgsL = parseArgs();

    String[] cmd = new String[5 + commandArgsL.size()];
    cmd[0] = "java";
    cmd[1] = "-cp";
    cmd[2] = cp;
    cmd[3] = "-Xmx" + heapSize;
    cmd[4] = className;  

    for(int i = 0; i < commandArgsL.size(); ++i){
      cmd[5 + i] = commandArgsL.get(i);
    }
    return cmd;
  }

  private void printCmd(String[] cmd){
    for(String s : cmd){ 
      s = s.replaceAll(File.pathSeparator, File.pathSeparator + "\n");
      System.out.print(s + " ");
      if(s.equals(className)) break;
    }
    System.out.print(commandArgs);
  }

  private void runTest(){
    if(className.equals("")){
      System.out.println(RED + "  No class selected, terminating check.");
      return;
    }
    String cp = baseCP
              + File.pathSeparator + rootPath + File.pathSeparator + cpAppend;

    String[] cmd = createCommand(cp);

    System.out.println("**************************************************"
                     + "**************************************************"
                     + "****************************");
    System.out.println("* Testing Base Program with command line:\n\t");
    printCmd(cmd);
    System.out.println("\n**************************************************"
                       + "**************************************************"
                       + "***************************");

    try { 
      long time = System.currentTimeMillis();
      Process p = Runtime.getRuntime().exec(cmd, null, new File(rootPath));
      System.setOut(testStream); 
      readExternalProcess(p, BLACK, RED);
      System.out.println("");
      System.setOut(stream); 
      if(v.stopButtonPressed) killAction();
      //if(p.exitValue() != 0) killAction(); 
      if(v.kill) {
        printStop();
        return;
      }
      float t =  ((System.currentTimeMillis() - time)/ 1000f);
      String stars = mkStars(t);
      System.out.println("********************************" + stars); 
      System.out.println("* Finished Testing Program " + YELLOW + "[" + t + "s] *");
      System.out.println("********************************" + stars + "\n"); 
    } catch(Exception e){
       e.printStackTrace();
    }
  }


  private void runProgram(){
    File tmpDir = tmpDirMap.get(absoluteFileName);
    String cp = tmpDir.getAbsolutePath() + File.pathSeparator + baseCP
              + File.pathSeparator + rootPath + File.pathSeparator + cpAppend;

    String[] cmd = createCommand(cp);

    System.out.println("**************************************************"
                     + "**************************************************"
                     + "****************************");
    System.out.println("* Running Instrumented Program with command line:\n\t");
    printCmd(cmd);
    System.out.println("\n**************************************************"
                       + "**************************************************"
                       + "***************************");

    try { 
      long time = System.currentTimeMillis();
      Process p = Runtime.getRuntime().exec(cmd, null, tmpDir);
      System.setOut(testStream); 
      readExternalProcess(p, BLACK, RED);
      System.out.println("");
      System.setOut(stream); 
      //if(p.exitValue() != 0) killAction(); 
      if(v.kill) return;
      float t =  ((System.currentTimeMillis() - time)/ 1000f);
      String stars = mkStars(t);
      System.out.println("*********************************************" + stars); 
      System.out.println("* Finished Running Instrumented Program " + YELLOW + "[" + t + "s] *");
      System.out.println("*********************************************" + stars + "\n"); 
    } catch(Exception e){
       e.printStackTrace();
    }
  }

  private void predict(){
    File tmpDir = tmpDirMap.get(absoluteFileName);
    String cp = tmpDir.getAbsolutePath() + File.pathSeparator + rootPath + File.pathSeparator 
      + cpAppend + File.pathSeparator + baseCP;
 
    String[] cmd = {"java","-ea", "-cp", cp, "-Xmx" + heapSize, "rvpredict.Main", "-app", className, "-d",
                           tmpDir.getAbsolutePath(),"-validate"};
    try {
      long time = System.currentTimeMillis();
      Process p  = Runtime.getRuntime().exec(cmd);
      readExternalProcess(p, BLACK, RED);
      if(p.exitValue() != 0) killAction(); 
      if(v.kill) return;
        } catch(Exception e){
       e.printStackTrace();
    }
    if(v.kill) return;
    float t =  ((System.currentTimeMillis() - totalTime)/ 1000f);
    String stars = mkStars(t);
    System.out.println("*******************************" + stars); 
    System.out.println("* Done " + YELLOW + "[" + t + "s total elapsed time] *");
    System.out.println("*******************************" + stars + "\n"); 
  }

  private static ArrayList<String> parseArgs(){
    ArrayList<String> commandArgsL = new ArrayList<String>();
    String parsed = "";
    boolean inString = false;
    for(int i = 0; i < commandArgs.length(); ++i){
      if(commandArgs.charAt(i) == '"'){
         inString = !inString;
      }
      else if(commandArgs.charAt(i) == ' '){
         if(inString) parsed += ' ';
         else {
           commandArgsL.add(parsed);
           parsed = "";
         }
      }
      else parsed += commandArgs.charAt(i);
    } 
    if(!"".equals(parsed)) commandArgsL.add(parsed);
    return commandArgsL;
  }

  private static void cleanUp(){
    File dir = tmpDirMap.get(absoluteFileName);
    //System.out.println("CLEANING UP");
    File[] fileList = dir.listFiles(new FilenameFilter() {
         public boolean accept(File dir, String name) {
            return name.endsWith(".rvpf") || name.startsWith("SuperList-");
         }
      });
    for(File f : fileList){
      f.delete();
    }
  }
}

class JTextPaneOutputStream extends OutputStream {
  JTextPane jtp;
  StyledDocument doc;
  Style style;
  static final int maxSize = 500000;
  static final int removeAmt = maxSize / 20;
  JTextPaneOutputStream(JTextPane jtp){
    super();
    this.jtp = jtp;
    doc = jtp.getStyledDocument();
    style = jtp.getStyle("black");
  }

  @Override
  public void write(int b) throws IOException {
    try{
      if(doc.getLength() > maxSize){
        doc.remove(0,removeAmt);
      }
      char c = (char) b;
      String buf = "";
      boolean n = false;
      switch(c){
        case '\n'          : style = jtp.getStyle("black");  
                             doc.insertString(doc.getLength(), String.valueOf(c), style);
                             jtp.setCaretPosition(doc.getLength());
                             return;
        case GUIMain.BLACK : style = jtp.getStyle("black");   return;
        case GUIMain.RED   : style = jtp.getStyle("red");     return;
        case GUIMain.GREEN : style = jtp.getStyle("green");   return;
        case GUIMain.BLUE  : style = jtp.getStyle("blue");    return;
        case GUIMain.YELLOW: style = jtp.getStyle("yellow");  return;
        case GUIMain.GREY  : style = jtp.getStyle("grey");    return;
        case '*'           : doc.insertString(doc.getLength(), String.valueOf(c), jtp.getStyle("blue"));
                             return;
        default            : doc.insertString(doc.getLength(), String.valueOf(c), style);
                             return;
      }
    }  catch (Exception e){
      e.printStackTrace();
      System.exit(1);
    }
  }
}

