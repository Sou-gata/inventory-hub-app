package in.gbtsolutions.inventoryhub.reports.gstr1;

import java.math.BigDecimal;

public class GSTR1Reconciler {

    public static GSTR1Models.ReconciliationResult reconcile(GSTR1Models.ReportData report) {
        GSTR1Models.ReconciliationResult result = new GSTR1Models.ReconciliationResult();

        BigDecimal suppliesTaxable = BigDecimal.ZERO;

        for (GSTR1Models.B2BRow row : report.b2bList) {
            suppliesTaxable = suppliesTaxable.add(row.taxableValue);
        }
        for (GSTR1Models.B2CRow row : report.b2cLargeList) {
            suppliesTaxable = suppliesTaxable.add(row.taxableValue);
        }
        for (GSTR1Models.B2CRow row : report.b2cOthersList) {
            suppliesTaxable = suppliesTaxable.add(row.taxableValue);
        }
        for (GSTR1Models.ExportRow row : report.exportList) {
            suppliesTaxable = suppliesTaxable.add(row.taxableValue);
        }
        for (GSTR1Models.NilExemptRow row : report.nilExemptList) {
            suppliesTaxable = suppliesTaxable.add(row.reportedValue);
        }

        BigDecimal hsnTaxable = BigDecimal.ZERO;
        for (GSTR1Models.HsnRow row : report.hsnB2bList) {
            hsnTaxable = hsnTaxable.add(row.taxableValue);
        }
        for (GSTR1Models.HsnRow row : report.hsnB2cList) {
            hsnTaxable = hsnTaxable.add(row.taxableValue);
        }

        result.expectedTaxable = suppliesTaxable;
        result.hsnTaxable = hsnTaxable;
        result.difference = suppliesTaxable.subtract(hsnTaxable).abs();
        result.isMatched = result.difference.compareTo(BigDecimal.valueOf(0.05)) <= 0;

        return result;
    }
}
