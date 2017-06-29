package com.runtimeverification.rvpredict.engine.main;

import com.runtimeverification.licensing.Licensing;

import static com.runtimeverification.licensing.Licensing.LICENSE_URL;

public class LicenseChecker {
    public static void validateOrDie(boolean promptForLicense) {
        Licensing licensingSystem = Licensing.fromUserConfigDirectory("predict");
        Licensing.LicenseStatus licenseStatus = licensingSystem.getLicenseStatus();
        if (licenseStatus == Licensing.LicenseStatus.VALID) {
            return;
        }
        if (promptForLicense) {
            licensingSystem.promptForLicenseIfNeeded(LicenseChecker::errorMessage);
        } else {
            System.out.println(errorMessage(licenseStatus));
            System.exit(1);
        }
        // Double check just in case.
        if (licensingSystem.getLicenseStatus() != Licensing.LicenseStatus.VALID) {
            System.exit(1);
        }
    }

    private static String errorMessage(Licensing.LicenseStatus licenseStatus) {
        switch (licenseStatus) {
            case NO_LICENSE:
                return "This product has no license on file.\n"
                        + "Please sign up for a license at " + LICENSE_URL + ","
                        + "then run this tool with --prompt-for-license.";
            case INVALID_OR_EXPIRED:
                return "Your license is invalid or expired.\n"
                        + "Please renew it at " + LICENSE_URL + ","
                        + "then run this tool with --prompt-for-license.";
            case VALID:
                return "Your license is valid.";
            default:
                assert false : "Unknown license status: " + licenseStatus;
                return "Unknown license status: " + licenseStatus;
        }
    }
}
