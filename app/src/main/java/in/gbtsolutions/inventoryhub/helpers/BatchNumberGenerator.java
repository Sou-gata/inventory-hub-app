package in.gbtsolutions.inventoryhub.helpers;

import java.util.Calendar;
import java.util.Locale;

import in.gbtsolutions.inventoryhub.repository.ProductBatchRepository;

public class BatchNumberGenerator {

    public interface GeneratorCallback {
        void onGenerated(String batchNo);
    }

    /**
     * Generates a batch number in format BN-YYDDD-NNN
     * where:
     * - YY = 2-digit current year (e.g. 26 for 2026)
     * - DDD = 3-digit Julian day / day of year (001-366)
     * - NNN = 3-digit zero-padded sequence for batches created today
     */
    public static void generate(ProductBatchRepository repository, GeneratorCallback callback) {
        if (repository == null) {
            if (callback != null) callback.onGenerated(generateSync(0));
            return;
        }

        repository.countBatchesCreatedToday(count -> {
            String batchNo = generateSync(count);
            if (callback != null) {
                callback.onGenerated(batchNo);
            }
        });
    }

    public static String generateSync(int countToday) {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR) % 100; // 2 digits
        int dayOfYear = cal.get(Calendar.DAY_OF_YEAR); // 1 to 366
        int seq = countToday + 1;

        return String.format(Locale.US, "BN-%02d%03d-%03d", year, dayOfYear, seq);
    }
}
