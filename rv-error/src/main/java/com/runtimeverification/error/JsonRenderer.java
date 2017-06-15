package com.runtimeverification.error;

import com.runtimeverification.error.data.LocationError;
import com.runtimeverification.error.data.Metadata;
import com.runtimeverification.error.data.StackError;

import java.io.PrintStream;

/**
 * Created by traian on 30.05.2017.
 */
public class JsonRenderer extends Renderer {
    public JsonRenderer(Metadata data) {
        super(data);
    }

    @Override
    protected void renderImpl(StackError error, PrintStream out) {
        renderJson(out, error.toJson());
    }

    @Override
    protected void renderImpl(LocationError error, PrintStream out) {
        renderJson(out, error.toJson());
    }

    private void renderJson(PrintStream stream, String json) {
        stream.println(json);
        stream.flush();
    }
}
