package in.gbtsolutions.inventoryhub.helpers;

import org.mindrot.jbcrypt.BCrypt;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HelperMethods {
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(10));
    }

    public static boolean comparePassword(String plainPassword, String hashPassword) {
        String compatibleHash = hashPassword;
        if (compatibleHash.startsWith("$2b$")) {
            compatibleHash = "$2a$" + compatibleHash.substring(4);
        }
        return BCrypt.checkpw(plainPassword, compatibleHash);
    }

    public static String generateInvoiceNo() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault());
        return "INV-" + sdf.format(new Date());
    }

    public static boolean validateGST(String gst) {
        if (gst == null || gst.isEmpty()) {
            return false;
        }
        String regex = "^[0-3][0-9][A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$";
        return gst.toUpperCase().matches(regex);
    }

    public static boolean isSameState(String gst1, String gst2) {
        if (!validateGST(gst1) || !validateGST(gst2)) return false;
        return gst1.substring(0, 2).equals(gst2.substring(0, 2));
    }

    public static boolean isSameStateByCode(String companyGst, String posStateCode) {
        if (companyGst == null || companyGst.trim().length() < 2 || posStateCode == null)
            return true;
        String companyCode = companyGst.trim().substring(0, 2);
        String pos = posStateCode.trim();
        if (pos.length() == 1) pos = "0" + pos;
        return companyCode.equals(pos);
    }

    public static String extractStateCode(String gstOrCode) {
        if (gstOrCode == null) return null;
        String trimmed = gstOrCode.trim();
        if (trimmed.length() >= 2) {
            String sub = trimmed.substring(0, 2);
            if (Character.isDigit(sub.charAt(0)) && Character.isDigit(sub.charAt(1))) {
                return sub;
            }
        }
        return null;
    }

    public static boolean isValidGSTIN(String gstin) {
        if (gstin == null) {
            return false;
        }
        gstin = gstin.trim().toUpperCase();
        if (gstin.length() != 15) {
            return false;
        }
        if (!gstin.matches("[0-9A-Z]{15}")) {
            return false;
        }
        String stateCode = gstin.substring(0, 2);
        String validStateCodes =
                "01,02,03,04,05,06,07,08,09,10,11,12,13,14,15,16," +
                        "17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32," +
                        "33,34,35,36,37,38,97,99";
        if (!validStateCodes.contains(stateCode)) {
            return false;
        }
        // Characters 3-12 must contain a valid PAN structure 5 letters + 4 digits + 1 letter
        String pan = gstin.substring(2, 12);
        if (!pan.matches("[A-Z]{5}[0-9]{4}[A-Z]")) {
            return false;
        }
        char entityNumber = gstin.charAt(12);
        if (!Character.isDigit(entityNumber) &&
                !(entityNumber >= 'A' && entityNumber <= 'Z')) {
            return false;
        }
        // 14th character must be Z
        if (gstin.charAt(13) != 'Z') {
            return false;
        }
        // Validate GSTIN checksum (15th character)
        final String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        int factor = 1;
        int sum = 0;
        for (int i = 0; i < 14; i++) {
            int charValue = chars.indexOf(gstin.charAt(i));
            if (charValue == -1) {
                return false;
            }
            int product = charValue * factor;
            sum += (product / 36) + (product % 36);
            factor = (factor == 1) ? 2 : 1;
        }
        int checkCode = (36 - (sum % 36)) % 36;
        char expectedChecksum = chars.charAt(checkCode);
        return gstin.charAt(14) == expectedChecksum;
    }
}
