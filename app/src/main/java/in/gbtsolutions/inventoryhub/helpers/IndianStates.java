package in.gbtsolutions.inventoryhub.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndianStates {

    public static class StateItem {
        public final String code;
        public final String name;

        public StateItem(String code, String name) {
            this.code = code;
            this.name = name;
        }

        public String getDisplayName() {
            return code + " - " + name;
        }

        @Override
        public String toString() {
            return getDisplayName();
        }
    }

    private static final List<StateItem> STATES = new ArrayList<>();
    private static final Map<String, String> CODE_TO_NAME = new HashMap<>();

    static {
        addState("01", "Jammu and Kashmir");
        addState("02", "Himachal Pradesh");
        addState("03", "Punjab");
        addState("04", "Chandigarh");
        addState("05", "Uttarakhand");
        addState("06", "Haryana");
        addState("07", "Delhi");
        addState("08", "Rajasthan");
        addState("09", "Uttar Pradesh");
        addState("10", "Bihar");
        addState("11", "Sikkim");
        addState("12", "Arunachal Pradesh");
        addState("13", "Nagaland");
        addState("14", "Manipur");
        addState("15", "Mizoram");
        addState("16", "Tripura");
        addState("17", "Meghalaya");
        addState("18", "Assam");
        addState("19", "West Bengal");
        addState("20", "Jharkhand");
        addState("21", "Odisha");
        addState("22", "Chhattisgarh");
        addState("23", "Madhya Pradesh");
        addState("24", "Gujarat");
        addState("26", "Dadra and Nagar Haveli and Daman and Diu");
        addState("27", "Maharashtra");
        addState("28", "Andhra Pradesh (Old)");
        addState("29", "Karnataka");
        addState("30", "Goa");
        addState("31", "Lakshadweep");
        addState("32", "Kerala");
        addState("33", "Tamil Nadu");
        addState("34", "Puducherry");
        addState("35", "Andaman and Nicobar Islands");
        addState("36", "Telangana");
        addState("37", "Andhra Pradesh (New)");
        addState("38", "Ladakh");
        addState("97", "Other Territory");
    }

    private static void addState(String code, String name) {
        StateItem item = new StateItem(code, name);
        STATES.add(item);
        CODE_TO_NAME.put(code, name);
    }

    public static List<StateItem> getAllStates() {
        return Collections.unmodifiableList(STATES);
    }

    public static String getStateName(String code) {
        if (code == null) return "Unknown";
        String trimmed = code.trim();
        if (trimmed.length() == 1) trimmed = "0" + trimmed;
        return CODE_TO_NAME.getOrDefault(trimmed, "State " + trimmed);
    }

    public static String getStateCodeFromGstin(String gstin) {
        if (gstin != null && gstin.trim().length() >= 2) {
            String code = gstin.trim().substring(0, 2);
            if (CODE_TO_NAME.containsKey(code)) {
                return code;
            }
        }
        return null;
    }

    public static StateItem findByCode(String code) {
        if (code == null) return null;
        String trimmed = code.trim();
        if (trimmed.length() == 1) trimmed = "0" + trimmed;
        for (StateItem item : STATES) {
            if (item.code.equalsIgnoreCase(trimmed)) {
                return item;
            }
        }
        return null;
    }
}
