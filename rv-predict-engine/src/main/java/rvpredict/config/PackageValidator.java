package rvpredict.config;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

/**
 * Class validating package patterns lists passed as parameters to the 
 * <code>--include</code> and <code>--exclude</code> options.
 */
public class PackageValidator implements IParameterValidator {

    @Override
    public void validate(String name, String value) throws ParameterException {
        value = value.trim();
        if (value.isEmpty()) return;
        final char firstChar = value.charAt(0);
        if (firstChar == '+') {
            value = value.substring(1);
        }
        String[] packages = value.split(",");
        for (String pkg : packages) {
            if (pkg.isEmpty()) {
                throw new ParameterException("Empty package specified for the " + name + " option.");
            }
            String[] pkgParts = pkg.trim().replace('.', '/').replace('\\', '/').split("/");
            for (String pkgPart : pkgParts) {
                if (pkgPart.isEmpty()) {
                    throw new ParameterException("Empty package part in " + pkg
                            + " specified as part of the " + name + " option.");
                }
                char[] chars = pkgPart.toCharArray();
                for (char c : chars) {
                    if (!(Character.isJavaIdentifierPart(c) || c == '*')) {
                        throw new ParameterException("Incorrect package component " + pkgPart
                                + " specified as part of the " + name + " option.");
                    }

                }
            }
        }

    }
}
