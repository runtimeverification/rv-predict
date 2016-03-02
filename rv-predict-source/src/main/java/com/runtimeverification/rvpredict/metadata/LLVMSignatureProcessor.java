package com.runtimeverification.rvpredict.metadata;

/**
 * Signature Processor specialized for the LLVM backend.
 *
 * @author TraianSF
 */
public class LLVMSignatureProcessor extends SignatureProcessor {
    String basePath = null;

    private static String extractFilePath(String locationSig) {
        int start = locationSig.indexOf(";file:") + ";file:".length();
        int end = locationSig.indexOf(";line:");
        return locationSig.substring(start,end);
    }

    @Override
    public String simplify(String s) {
        final String prefix = basePath.substring(0, 1 + basePath.lastIndexOf('/'));
        if (prefix.length() > 1) {
            s = s.replace(prefix, "");
        }
        return s.replace("fn:","").replace(";file:"," ").replace(";line:",":");
    }

    @Override
    public void process(String locSig) {
        String prefix = extractFilePath(locSig);
        if (prefix == null) {
            return;
        }
        if (basePath == null) {
            basePath = prefix;
        }
        int i = 0;
        int minLength = Math.min(basePath.length(), prefix.length());
        while (i <  minLength && basePath.charAt(i) == prefix.charAt(i)) {
            i++;
        }
        basePath = basePath.substring(0, i);
    }

    @Override
    public void reset() {
        basePath = null;
    }
}
