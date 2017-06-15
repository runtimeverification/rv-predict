// Copyright (c) 2017 Runtime Verification, Inc. (RV-Match team). All Rights Reserved.
package com.runtimeverification.error;

import com.runtimeverification.error.data.Citation;
import com.runtimeverification.error.data.ErrorCategory;
import com.runtimeverification.error.data.LocationError;
import com.runtimeverification.error.data.Metadata;
import com.runtimeverification.error.data.StackError;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 * Created by dwightguth on 1/4/17.
 */
public class CSVRenderer extends Renderer {
    public CSVRenderer(Metadata data) {
        super(data);
    }

    @Override
    protected void renderImpl(StackError error, PrintStream out) {
        CSVPrinter printer = getCSVPrinter(out);
        String[] strings = getCommonFields(error.error_id, error.description, error.category, error.citations);

        StringBuilder reason = new StringBuilder();
        error.traces.subList(1, error.traces.size()).forEach(t -> getConsole().renderTrace(reason, t));
        strings[4] = reason.toString();

        StringBuilder trace = new StringBuilder();
        if (error.traces.size() > 0) {
            getConsole().renderTrace(trace, error.traces.get(0));
        }
        strings[5] = trace.toString();

        printRecord(printer, strings);
    }

    private void printRecord(CSVPrinter printer, String[] strings) {
        try {
            printer.printRecord(strings);
            printer.flush();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(2);
            throw new AssertionError("unreachable");
        }
    }

    private String[] getCommonFields(String error_id, String description, ErrorCategory category, List<Citation> citations) {
        String[] strings = new String[6];
        strings[0] = error_id;
        strings[1] = description;
        strings[2] = renderErrorCategory(category);

        StringBuilder citationsSb = new StringBuilder();
        citations.forEach(c -> renderCitation(citationsSb, c));
        strings[3] = citationsSb.toString();
        return strings;
    }

    private void renderCitation(StringBuilder sb, Citation citation) {
        sb.append("\n");
        sb.append(citation.document);
        sb.append(",");
        sb.append(citation.section);
        sb.append(",");
        if (citation.paragraph != null) {
            sb.append(citation.paragraph);
        }
        sb.append(",");
        getConsole().renderCitationUrl(sb, citation);
    }

    private CSVPrinter getCSVPrinter(PrintStream out) {
        try {
            return new CSVPrinter(out, CSVFormat.EXCEL);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(2);
            throw new AssertionError("unreachable");
        }
    }

    @Override
    protected void renderImpl(LocationError error, PrintStream out) {
        CSVPrinter printer = getCSVPrinter(out);
        String[] strings = getCommonFields(error.error_id, error.description, error.category, error.citations);
        strings[4] = "";

        StringBuilder loc = new StringBuilder();
        getConsole().renderLoc(loc, error.loc.loc);
        strings[5] = loc.toString();

        printRecord(printer, strings);
    }


}
