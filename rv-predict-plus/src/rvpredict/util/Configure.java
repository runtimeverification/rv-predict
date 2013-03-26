package rvpredict.util;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Configure{
  private static final String RESOURCE_BUNDLE= "rvpredict.util.Conf";
  private static ResourceBundle fgResourceBundle= ResourceBundle.getBundle(RESOURCE_BUNDLE);

  public Configure() {
  }

  public static String getString(String key) {
    try {
      return fgResourceBundle.getString(key);
    } catch (MissingResourceException e) {
      return "!" + key + "!";
    }
  }

}
// vim: tw=100:sw=2
