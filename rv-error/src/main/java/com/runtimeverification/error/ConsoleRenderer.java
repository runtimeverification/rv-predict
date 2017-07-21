// Copyright (c) 2016-2017 Runtime Verification, Inc. (RV-Match team). All Rights Reserved.
package com.runtimeverification.error;

import com.runtimeverification.error.data.Citation;
import com.runtimeverification.error.data.ErrorCategory;
import com.runtimeverification.error.data.Frame;
import com.runtimeverification.error.data.Location;
import com.runtimeverification.error.data.LocationError;
import com.runtimeverification.error.data.Lock;
import com.runtimeverification.error.data.Metadata;
import com.runtimeverification.error.data.StackError;
import com.runtimeverification.error.data.StackTrace;
import com.runtimeverification.error.data.StackTraceComponent;

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
    protected void renderImpl(StackError error, PrintStream out) {
        StringBuilder sb = new StringBuilder();
        sb.append(" ");
        sb.append(error.description);
        newline(sb);
        error.stack_traces.forEach(t -> renderTrace(sb, t));
        renderIdAndCategory(error.category, error.error_id, sb);
        error.citations.forEach(c -> renderCitation(sb, c));
        out.print(sb.toString());
    }

    void renderTrace(StringBuilder sb, StackTrace trace) {
        trace.components.forEach(c -> renderComponent(sb, c));
        if (trace.thread_id != null) {
            if (trace.thread_created_by != null) {
                sb.append("  Thread ").append(trace.thread_id).append(" created by thread ").append(trace.thread_created_by);
                newline(sb);
                renderFrame(sb, "at", trace.thread_created_at, 0);
            } else {
                sb.append("  Thread ").append(trace.thread_id).append(" is the main thread");
                newline(sb);
            }
            newline(sb);
        }
    }

    private void renderComponent(StringBuilder sb, StackTraceComponent component) {
        if (component.description != null) {
            sb.append("  ");
            sb.append(component.description);
            newline(sb);
        }
        if (component.frames.size() > 0) {
            int numElided = 0;
            numElided = renderFrame(sb, "at", component.frames.get(0), numElided);
            for (Frame f : component.frames.subList(1, component.frames.size())) {
                numElided = renderFrame(sb, "by", f, numElided);
            }
            if (numElided > 0) {
                renderElided(sb, numElided);
            }
        }
    }

    private int renderFrame(StringBuilder sb, String start, Frame frame, int numElided) {
        if (frame.elided) {
            return numElided+1;
        } else if (numElided > 0) {
            renderElided(sb, numElided);
        }
        sb.append("   ");
        sb.append(start);
        sb.append(" ");
        sb.append(frame.symbol);
        sb.append("(");
        renderLoc(sb, frame.loc);
        sb.append(")");
        newline(sb);
        frame.locks.forEach(l -> renderLock(sb, l));
        return 0;
    }

    private void renderElided(StringBuilder sb, int numElided) {
        sb.append("   ... ").append(numElided).append(" library frame");
        if (numElided > 1)
            sb.append("s");
        newline(sb);
    }

    private void renderLock(StringBuilder sb, Lock lock) {
        sb.append("   - locked ").append(lock.id).append(" at ").append(lock.locked_at.symbol).append("(");
        renderLoc(sb, lock.locked_at.loc);
        sb.append(")");
        newline(sb);
    }

    @Override
    protected void renderImpl(LocationError error, PrintStream out) {
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
