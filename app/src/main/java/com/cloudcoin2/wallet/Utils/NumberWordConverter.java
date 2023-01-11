package com.cloudcoin2.wallet.Utils;

import java.util.Random;

public class NumberWordConverter {
    public static final String[] units = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven",
            "Eight", "Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen",
            "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };

    public static final String[] tens = {
            "",        // 0
            "",        // 1
            "Twenty",  // 2
            "Thirty",  // 3
            "Forty",   // 4
            "Fifty",   // 5
            "Sixty",   // 6
            "Seventy", // 7
            "Eighty",  // 8
            "Ninety"   // 9
    };

    public static String convert(final int n) {
        if (n < 0) {
            return "minus " + convert(-n);
        }

        if (n < 20) {
            return units[n];
        }

        if (n < 100) {
            return tens[n / 10] + ((n % 10 != 0) ? " " : "") + units[n % 10];
        }

        if (n < 1000) {
            return units[n / 100] + " Hundred" + ((n % 100 != 0) ? " " : "") + convert(n % 100);
        }

        if (n < 1000000) {
            return convert(n / 1000) + " Thousand" + ((n % 1000 != 0) ? " " : "") + convert(n % 1000);
        }

        if (n < 1000000000) {
            return convert(n / 1000000) + " Million" + ((n % 1000000 != 0) ? " " : "") + convert(n % 1000000);
        }

        return convert(n / 1000000000) + " Billion"  + ((n % 1000000000 != 0) ? " " : "") + convert(n % 1000000000);
    }

    public static void main(final String[] args) {
        final Random generator = new Random();

        int n;
        for (int i = 0; i < 20; i++) {
            n = generator.nextInt(Integer.MAX_VALUE);

            System.out.printf("%10d = '%s'%n", n, convert(n));
        }

        n = 1000;
        System.out.printf("%10d = '%s'%n", n, convert(n));

        n = 2000;
        System.out.printf("%10d = '%s'%n", n, convert(n));

        n = 10000;
        System.out.printf("%10d = '%s'%n", n, convert(n));

        n = 11000;
        System.out.printf("%10d = '%s'%n", n, convert(n));

        n = 999999999;
        System.out.printf("%10d = '%s'%n", n, convert(n));

        n = Integer.MAX_VALUE;
        System.out.printf("%10d = '%s'%n", n, convert(n));
    }
}
