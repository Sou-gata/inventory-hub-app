package in.gbtsolutions.inventoryhub.helpers;

public class NumberToWordsHelper {

    private static final String[] UNITS = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
            "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    public static String convertToIndianCurrencyWords(double amount) {
        long rounded = Math.round(amount);
        if (rounded == 0) {
            return "Rupees Zero Only";
        }

        StringBuilder sb = new StringBuilder();
        if (rounded < 0) {
            sb.append("Minus ");
            rounded = Math.abs(rounded);
        }

        sb.append("Rupees ");
        sb.append(convert(rounded));
        sb.append(" Only");

        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    private static String convert(long n) {
        if (n == 0) return "";

        StringBuilder sb = new StringBuilder();

        if (n >= 10000000) { // Crores
            sb.append(convert(n / 10000000)).append(" Crore ");
            n %= 10000000;
        }

        if (n >= 100000) { // Lakhs
            sb.append(convert(n / 100000)).append(" Lakh ");
            n %= 100000;
        }

        if (n >= 1000) { // Thousands
            sb.append(convert(n / 1000)).append(" Thousand ");
            n %= 1000;
        }

        if (n >= 100) { // Hundreds
            sb.append(convert(n / 100)).append(" Hundred ");
            n %= 100;
        }

        if (n > 0) {
            if (n < 20) {
                sb.append(UNITS[(int) n]).append(" ");
            } else {
                sb.append(TENS[(int) (n / 10)]);
                if (n % 10 > 0) {
                    sb.append("-").append(UNITS[(int) (n % 10)]);
                }
                sb.append(" ");
            }
        }

        return sb.toString().trim();
    }
}
