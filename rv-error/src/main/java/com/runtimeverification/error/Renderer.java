// Copyright (c) 2016 Runtime Verification, Inc. (RV-Match team). All Rights Reserved.
package com.runtimeverification.error;

import com.runtimeverification.error.data.Citation;
import com.runtimeverification.error.data.Condition;
import com.runtimeverification.error.data.ErrorCategory;
import com.runtimeverification.error.data.Frame;
import com.runtimeverification.error.data.Location;
import com.runtimeverification.error.data.LocationError;
import com.runtimeverification.error.data.Metadata;
import com.runtimeverification.error.data.StackError;
import com.runtimeverification.error.data.Suppression;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by dwightguth on 12/28/16.
 */
public abstract class Renderer {
    protected final Metadata data;

    private final Set<String> previous_errors = new HashSet<>();
    private CSVRenderer csv;
    private ConsoleRenderer console;
    private Renderer CSV;

    public Renderer(Metadata data) {
        this.data = data;
        for (int i = 0; i < data.previous_errors.size() - 1; i++) {
            previous_errors.add(data.previous_errors.get(i));
        }
    }

    public boolean render(StackError error, String json) {
        Frame topFrame;
        if (error.traces.size() == 0 || error.traces.get(0).frames.size() == 0) {
            topFrame = new Frame();
        } else {
            topFrame = error.traces.get(0).frames.get(0);
        }
        error.error_id = getRealErrorId(error.error_id, error.category);
        error.citations = getCitations(error.error_id);
        if (suppress(error.category, error.error_id, topFrame.loc, topFrame.symbol, json)) {
            return false;
        }
        try (PrintStream stream = getOutStream(data.output)) {
            renderImpl(error, stream);
        }
        try (PrintStream stream = getOutStream(new File(System.getProperty("user.home"), ".kcc-report.csv").getAbsolutePath())) {
            getCSV().renderImpl(error, stream);
        }
        return data.fatal_errors;
    }

    private PrintStream getOutStream(String path) {
        if (path == null) {
            return System.err;
        } else {
            try {
                return new PrintStream(new FileOutputStream(path, true));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.exit(2);
                throw new AssertionError("unreachable");
            }
        }
    }

    private ArrayList<Citation> getCitations(String error_id) {
        ArrayList<Citation> array = new ArrayList<>();
        try {
            CSVParser parser = CSVParser.parse(getClass().getResource("/citations.csv"), StandardCharsets.UTF_8, CSVFormat.EXCEL);
            boolean found = false;
            for (CSVRecord record : parser) {
                if (record.get(0).equals(error_id)) {
                    Citation c = new Citation();
                    c.document = record.get(1);
                    c.section = record.get(2);
                    String paragraph = record.get(3);
                    if (paragraph.equals("")) {
                        c.paragraph = null;
                    } else {
                        c.paragraph = paragraph;
                    }
                    array.add(c);
                    found = true;
                } else if (found) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return array;
    }

    protected abstract void renderImpl(StackError error, PrintStream out);

    public boolean render(LocationError error, String json) {
        error.error_id = getRealErrorId(error.error_id, error.category);
        error.citations = getCitations(error.error_id);
        if (suppress(error.category, error.error_id, error.loc.loc, error.loc.symbol, json)) {
            return false;
        }
        try (PrintStream stream = getOutStream(data.output)) {
            renderImpl(error, stream);
        }
        try (PrintStream stream = getOutStream(new File(System.getProperty("user.home"), ".kcc-report.csv").getAbsolutePath())) {
            getCSV().renderImpl(error, stream);
        }
        return data.fatal_errors;
    }

    private String getRealErrorId(String error_id, ErrorCategory category) {
        switch (category.tag()) {
        case CONDITIONALLYSUPPORTED:
            return "CND-" + error_id;
        case CONSTRAINTVIOLATION:
            return "CV-" + error_id;
        case ILLFORMED:
            return "ILF-" + error_id;
        case IMPLEMENTATIONDEFINED:
            switch (category.getImplementationDefined().tag()) {
            case C:
                return "IMPL-" + error_id;
            case CPP:
                return "IMPL++-" + error_id;
            }
        case LINTERROR:
            return "L-" + error_id;
        case SYNTAXERROR:
            switch (category.getSyntaxError().tag()) {
            case C:
                return "SE-" + error_id;
            case CPP:
                return "SE++-" + error_id;
            }
        case UNDEFINED:
            switch (category.getUndefined().tag()) {
            case C:
                return "UB-" + error_id;
            case CPP:
                return "UB++-" + error_id;
            }
        case UNDERSPECIFIED:
            switch (category.getUnderspecified().tag()) {
            case C:
                return "DR-" + error_id;
            case CPP:
                return "DR++-" + error_id;
            }
        case UNSPECIFIED:
            switch (category.getUnspecified().tag()) {
            case C:
                return "USP-" + error_id;
            case CPP:
                return "USP++-" + error_id;
            }
        default:
            throw new AssertionError("unexpected error category");
        }
    }

    protected String renderErrorCategory(ErrorCategory category) {
        switch (category.tag()) {
        case CONDITIONALLYSUPPORTED:
            return "Conditionally-supported behavior";
        case CONSTRAINTVIOLATION:
            return "Constraint violation";
        case ILLFORMED:
            return "Ill-formed program";
        case IMPLEMENTATIONDEFINED:
            return "Implementation defined behavior";
        case LINTERROR:
            return "Possible unintended behavior";
        case SYNTAXERROR:
            return "Syntax error";
        case UNDEFINED:
            return "Undefined behavior";
        case UNDERSPECIFIED:
            return "Behavior underspecified by standard";
        case UNSPECIFIED:
            return "Unspecified value or behavior";
        default:
            throw new AssertionError("unsupported error category");
        }
    }

    protected abstract void renderImpl(LocationError error, PrintStream out);

    public CSVRenderer getCSV() {
        CSVRenderer csv = this.csv;
        if (csv == null) {
            if (this instanceof CSVRenderer) {
                csv = (CSVRenderer) this;
            } else {
                csv = new CSVRenderer(data);
            }
            this.csv = csv;
        }
        return csv;
    }

    public ConsoleRenderer getConsole() {
        ConsoleRenderer console = this.console;
        if (console == null) {
            if (this instanceof ConsoleRenderer) {
                console = (ConsoleRenderer) this;
            } else {
                console = new ConsoleRenderer(data);
            }
            this.console = console;
        }
        return console;
    }

    private class SuppressState {
        boolean suppressErrorId = false;
        boolean suppressSystemHeader = false;
        boolean suppressLoc = false;
        boolean suppressDuplicate = false;
    }

    private boolean suppress(ErrorCategory category, String error_id, Location loc,
                             String symbol, String json) {
        SuppressState state = new SuppressState();
        boolean isDuplicate = previous_errors.contains(json);
        for (Suppression suppression : builtinSuppress()) {
            processSuppression(suppression, state, category, error_id, loc, symbol, isDuplicate);
        }
        if (loc != null) {
            for (Suppression suppression : cacheSuppress(loc.abs_file)) {
                processSuppression(suppression, state, category, error_id, loc, symbol, isDuplicate);
            }
        }
        for (Suppression suppression : data.suppressions) {
            processSuppression(suppression, state, category, error_id, loc, symbol, isDuplicate);
        }
        return state.suppressErrorId || state.suppressSystemHeader || state.suppressLoc || state.suppressDuplicate;
    }

    private List<Suppression> cacheSuppress(String absPath) {
        try {
            Path path = Paths.get(System.getProperty("user.home"), ".rvsuppress", absPath, "ifdef.json");
            String dataFile = IOUtils.toString(new FileInputStream(path.toFile()), "UTF-8");
            Metadata data = new Metadata(dataFile);
            return data.suppressions;
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    private void processSuppression(Suppression suppression, SuppressState state, ErrorCategory category,
                                    String error_id, Location loc, String symbol, boolean isDuplicate) {
        switch (suppression.condition.tag()) {
        case CATEGORY:
            if (category.tag().equals(suppression.condition.getCategory().tag())) {
                state.suppressErrorId = suppression.suppress;
            }
            break;
        case DUPLICATE:
            if (isDuplicate == suppression.condition.getDuplicate()) {
                state.suppressDuplicate = suppression.suppress;
            }
        case ERRORID:
            if (error_id.equals(suppression.condition.getErrorId())) {
                state.suppressErrorId = suppression.suppress;
            }
            break;
        case FILE:
            if (loc != null && matchesGlob(loc.rel_file, loc.abs_file, suppression.condition.getFile())) {
                state.suppressLoc = suppression.suppress;
            }
            break;
        case LINE:
            if (loc != null && loc.line >= suppression.condition.getLine().start_line
                    && loc.line <= suppression.condition.getLine().end_line
                    && (suppression.condition.getLine().file == null
                    || loc.rel_file.equals(suppression.condition.getLine().file))) {
                state.suppressLoc = suppression.suppress;
            }
            break;
        case SYMBOL:
            if (symbol != null && Pattern.compile(suppression.condition.getSymbol()).matcher(symbol).matches()) {
                state.suppressLoc = suppression.suppress;
            }
            break;
        case SYSTEMHEADER:
            if (loc != null && loc.system_header == suppression.condition.getSystemHeader()) {
                state.suppressSystemHeader = suppression.suppress;
            }
            break;
        default:
            // TODO(dwightguth): implement ifdef checking
            break;
        }
    }

    private boolean matchesGlob(String relFile, String absFile, String glob) {
        try {
            String workingDir = absFile.substring(0, absFile.indexOf(relFile));
            return new ProcessBuilder().command("rv-fileglob", absFile, workingDir, glob).start().waitFor() == 0;
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Suppression[] builtinSuppress() {
        Suppression suppressSystemHeader = new Suppression();
        suppressSystemHeader.suppress = true;
        suppressSystemHeader.condition = new Condition();
        suppressSystemHeader.condition.setSystemHeader(true);
        Suppression suppressLint = new Suppression();
        suppressLint.suppress = true;
        suppressLint.condition = new Condition();
        ErrorCategory lint = new ErrorCategory();
        lint.setLintError();
        suppressLint.condition.setCategory(lint);
        Suppression suppressDuplicate = new Suppression();
        suppressDuplicate.suppress = true;
        suppressDuplicate.condition = new Condition();
        suppressDuplicate.condition.setDuplicate(true);
        return new Suppression[] {suppressSystemHeader, suppressLint, suppressDuplicate};
    }

    public static Renderer of(Metadata data) {
        switch(data.format.tag()) {
        case CONSOLE:
            return new ConsoleRenderer(data);
        case CSV:
            return new CSVRenderer(data);
        default:
            throw new AssertionError("unimplemented renderer");
        }
    }
}
