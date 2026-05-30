package com.thedal.thedal_app.util;

public class RandomTokenGenerator {
    public static String generateToken(int length) {
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "0123456789"
                + "abcdefghijklmnopqrstuvxyz";

        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int index = (int) (AlphaNumericString.length() * Math.random());
            sb.append(AlphaNumericString.charAt(index));
        }

        return sb.toString();
    }

    public static String generateOTP(int length) {

        String NumericString = "0123456789";
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int index = (int) (NumericString.length() * Math.random());
            sb.append(NumericString.charAt(index));
        }

        return sb.toString();

    }
}
