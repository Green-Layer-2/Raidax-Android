package com.cloudcoin2.wallet.Utils;

import java.util.ArrayList;
import java.util.List;

public class Denominations {
    public int decimal;
    public double fraction;

    Denominations() {

    }

    private String stringRepresentation;

    public String getStringRepresentation() {
        return stringRepresentation;
    }

    public static String getDenomination(int exponent) {
        List<Denominations> denominations = Denominations.getDenominationsList();
        for (Denominations d : denominations) {
            if (d.decimal == exponent) {
                return d.getStringRepresentation();
            }
        }
        return null; // or throw an exception, if appropriate
    }

    public static List<Denominations> getDenominationsList() {
        List<Denominations> denominations = new ArrayList<>();
        denominations.add(new Denominations(-8, .00000001, "00_000_001"));
        denominations.add(new Denominations(-7, .0000001, "00_000_010"));
        denominations.add(new Denominations(-6, .000001, "00_000_100"));
        denominations.add(new Denominations(-5, .00001, "00_001_000"));
        denominations.add(new Denominations(-4, .0001, "00_010_000"));
        denominations.add(new Denominations(-3, .001, "00_100_000"));
        denominations.add(new Denominations(-2, .01, "01_000_000"));
        denominations.add(new Denominations(-1, .1, "10_000_000"));
        denominations.add(new Denominations(0, 1, "10_000_000"));
        denominations.add(new Denominations(1, 10, "10_000_000"));
        denominations.add(new Denominations(2, 100, "10_000_000"));
        denominations.add(new Denominations(3, 1000, "10_000_000"));
        denominations.add(new Denominations(4, 10000, "10_000_000"));
        denominations.add(new Denominations(5, 100000, "10_000_000"));
        denominations.add(new Denominations(6, 1000000, "10_000_00"));
        return denominations;
    }

    public Denominations(int exponent, double fraction, String stringRepresentation) {
        this.decimal = exponent;
        this.fraction = fraction;
        this.stringRepresentation = stringRepresentation;
    }

}
