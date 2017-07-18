// Copyright (c) 2016 Runtime Verification, Inc. (RV-Match team). All Rights Reserved.
package com.runtimeverification.error;

import com.martiansoftware.nailgun.NGContext;
import com.runtimeverification.error.data.LocationError;
import com.runtimeverification.error.data.Metadata;
import com.runtimeverification.error.data.StackError;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class RVError {

  public static void run(String data_file, String env_var, String working_dir) throws IOException, JSONException {
    String json = IOUtils.toString(System.in, "UTF-8");
    try {
      StackError error = new StackError(json);
      boolean isFatal = getRenderer(data_file, env_var, working_dir, json).render(error, json);
      System.exit(isFatal ? 1 : 0);
    } catch (JSONException e) {
      try {
        LocationError error = new LocationError(json);
        boolean isFatal = getRenderer(data_file, env_var, working_dir, json).render(error, json);
        System.exit(isFatal ? 1 : 0);
      } catch (JSONException e2) {
        System.err.println(json);
        e.printStackTrace();
        e2.printStackTrace();
        System.exit(2);
      }
    }
  }

  public static void main(String[] args) throws IOException, JSONException {
    run(args[0], System.getenv("RV_ISSUE_REPORT"), ".");
  }

  public static void nailMain(NGContext context) throws IOException, JSONException {
    run(context.getArgs()[0], (String)context.getEnv().get("RV_ISSUE_REPORT"), context.getWorkingDirectory());
  }

  private static Renderer getRenderer(String data_file, String env_var, String working_dir, String json) throws IOException {
    String dataFile = IOUtils.toString(new FileInputStream(data_file), "UTF-8");
    Metadata data = new Metadata(dataFile);
    if (env_var != null) {
      String ext = FilenameUtils.getExtension(env_var);
      File out = new File(env_var);
      if (out.isAbsolute()) {
        data.output = env_var;
      } else {
        data.output = new File(working_dir, env_var).getAbsolutePath();
      }
      if (ext.equals("json")) {
        data.format.setJSON();
      } else if (ext.equals("csv")) {
        data.format.setCSV();
      }
    }
    data.previous_errors.add(json);
    try (OutputStream os = new FileOutputStream(data_file)) {
      IOUtils.write(data.toJson(), os);
    }
    return Renderer.of(data);
  }
}

