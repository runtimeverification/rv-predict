// Copyright (c) 2016 Runtime Verification, Inc. (RV-Match team). All Rights Reserved.
package com.runtimeverification.error;

import com.runtimeverification.error.data.Citation;
import com.runtimeverification.error.data.ErrorCategory;
import com.runtimeverification.error.data.Frame;
import com.runtimeverification.error.data.Location;
import com.runtimeverification.error.data.LocationError;
import com.runtimeverification.error.data.Metadata;
import com.runtimeverification.error.data.StackError;
import com.runtimeverification.error.data.Trace;

import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by dwightguth on 12/28/16.
 */
public class ConsoleRenderer extends Renderer {
    public ConsoleRenderer(Metadata data) {
        super(data);
    }

    @Override
    protected void renderImpl(StackError error, PrintStream out, String json) {
        StringBuilder sb = new StringBuilder();
        sb.append(" ");
        sb.append(error.description);
        newline(sb);
        error.traces.forEach(t -> renderTrace(sb, t));
        renderIdAndCategory(error.category, error.error_id, sb);
        error.citations.forEach(c -> renderCitation(sb, c));
        out.print(sb.toString());
    }

    void renderTrace(StringBuilder sb, Trace trace) {
        if (trace.description != null) {
            sb.append("  ");
            sb.append(trace.description);
            newline(sb);
        }
        if (trace.frames.size() > 0) {
            renderFrame(sb, "at", trace.frames.get(0));
            trace.frames.subList(1, trace.frames.size()).forEach(f -> renderFrame(sb, "by", f));
        }
    }

    private void renderFrame(StringBuilder sb, String start, Frame frame) {
        sb.append("   ");
        sb.append(start);
        sb.append(" ");
        sb.append(frame.symbol);
        sb.append("(");
        renderLoc(sb, frame.loc);
        sb.append(")");
        newline(sb);
    }

    @Override
    protected void renderImpl(LocationError error, PrintStream out, String json) {
        StringBuilder sb = new StringBuilder();
        renderLoc(sb, error.loc.loc);
        sb.append(": ");
        renderErrorSupercategory(sb, error.category);
        sb.append(error.description);
        newline(sb);
        renderIdAndCategory(error.category, error.error_id, sb);
        error.citations.forEach(c -> renderCitation(sb, c));
        out.print(sb.toString());
    }

    private void renderIdAndCategory(ErrorCategory category, String error_id, StringBuilder sb) {
        sb.append("  ");
        sb.append(renderErrorCategory(category));
        sb.append(" (");
        sb.append(error_id);
        sb.append(").");
        newline(sb);
    }

    private void renderCitation(StringBuilder sb, Citation citation) {
        sb.append("   see ");
        sb.append(citation.document);
        sb.append(" section ");
        sb.append(citation.section);
        if (citation.paragraph != null) {
            sb.append(":");
            sb.append(citation.paragraph);
        }
        renderCitationUrl(sb, citation);
        newline(sb);
    }

    void renderCitationUrl(StringBuilder sb, Citation citation) {
        try {
            URI uri = new URI(
                    "http",
                    "rvdoc.org",
                    "/" + citation.document + "/" + citation.section, null
            );
            sb.append(" ");
            sb.append(uri.toString());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void newline(StringBuilder sb) {
        if (data.message_length != 0) {
            sb.append('\n');
        }
    }

    private void renderErrorSupercategory(StringBuilder sb, ErrorCategory category) {
        switch(category.tag()) {
        case IMPLEMENTATIONDEFINED:
        case LINTERROR:
            sb.append("warning: ");
            break;
        default:
            sb.append("error: ");
            break;
        }
    }

    void renderLoc(StringBuilder sb, Location loc) {
        if (loc == null) {
            sb.append("<unknown>");
            return;
        }
        sb.append(loc.rel_file);
        sb.append(":");
        sb.append(loc.line);
        if (loc.column != null) {
            sb.append(":");
            sb.append(loc.column);
        }
    }
}
