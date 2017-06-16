package com.runtimeverification.rvpredict.engine.main;

import com.runtimeverification.licensing.Licensing;
import com.runtimeverification.licensing.RVLicenseCache;
import com.runtimeverification.rvpredict.config.Configuration;

public class LicenseChecker {
    private static final String LICENSE_URL = "https://runtimeverification.com/licensing";

    public static void validateOrDie(boolean promptForLicense) {
        Licensing licensingSystem = new Licensing(Configuration.AGENT_RESOURCE_PATH, "predict");
        LicenseStatus licenseStatus = getLicenseStatus(licensingSystem);
        if (licenseStatus != LicenseStatus.VALID && promptForLicense) {
            licensingSystem.promptForLicense();
            licenseStatus = getLicenseStatus(licensingSystem);
        }
        switch (licenseStatus) {
            case NO_LICENSE:
                System.err.println("This product has no license on file.");
                System.err.println("Please sign up for a license at " + LICENSE_URL + ",");
                System.err.println("then run this tool with --prompt-for-license.");
                System.exit(1);
            case EXPIRED:
                System.err.println("Your license is invalid or expired.");
                System.err.println("Please renew it at " + LICENSE_URL + ",");
                System.err.println("then run this tool with --prompt-for-license.");
                System.exit(1);
            case VALID:
                break;
            default:
                assert false : "Unknown license status: " + licenseStatus;
        }
    }

    private enum LicenseStatus {
        VALID,
        NO_LICENSE,
        EXPIRED
    }

    private static LicenseStatus getLicenseStatus(Licensing licensingSystem) {
        RVLicenseCache licenseCache = licensingSystem.getLicenseCache();
        if (!licenseCache.isLicenseCached()) {
            return LicenseStatus.NO_LICENSE;
        }
        if (!licenseCache.isLicensed()) {
            return LicenseStatus.EXPIRED;
        }
        return LicenseStatus.VALID;
    }
}
