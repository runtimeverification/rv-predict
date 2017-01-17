// Copyright (c) 2016 Runtime Verification, Inc. (RV-Match team). All Rights Reserved.
package com.runtimeverification.error;

import com.runtimeverification.error.data.LocationError;
import com.runtimeverification.error.data.Metadata;
import com.runtimeverification.error.data.StackError;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class RVError {

  public static void main(String[] args) throws IOException, JSONException {
    String json = IOUtils.toString(System.in, "UTF-8");
    try {
      StackError error = new StackError(json);
      boolean isFatal = getRenderer(args[0], json).render(error, json);
      System.exit(isFatal ? 1 : 0);
    } catch (JSONException e) {
      try {
        LocationError error = new LocationError(json);
        boolean isFatal = getRenderer(args[0], json).render(error, json);
        System.exit(isFatal ? 1 : 0);
      } catch (JSONException e2) {
        e.printStackTrace();
        e2.printStackTrace();
        System.exit(2);
      }
    }
  }

  private static Renderer getRenderer(String data_file, String json) throws IOException {
    String dataFile = IOUtils.toString(new FileInputStream(data_file), "UTF-8");
    Metadata data = new Metadata(dataFile);
    data.previous_errors.add(json);
    try (OutputStream os = new FileOutputStream(data_file)) {
      IOUtils.write(data.toJson(), os);
    }
    return Renderer.of(data);
  }
}

