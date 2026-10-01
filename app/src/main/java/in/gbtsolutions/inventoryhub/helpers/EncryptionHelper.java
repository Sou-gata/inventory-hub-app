package in.gbtsolutions.inventoryhub.helpers;

import android.util.Base64;

import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import in.gbtsolutions.inventoryhub.Configurations;

public class EncryptionHelper {
    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
    private static final String mkFixed = Configurations.mkFixed;
    private static final String ivFixed = Configurations.ivFixed;

    public static String hexToDecimal(String hexString) {
        int decimalValue = Integer.parseInt(hexString, 16);
        return String.valueOf(decimalValue);
    }

    public static String encrypt(String input) throws Exception {
        byte[] plaintext = input.getBytes(StandardCharsets.UTF_8);
        byte[] iv = hexToBytes(ivFixed);
        byte[] tdesKey = hexToBytes(mkFixed);
        if (tdesKey.length == 16) {
            byte[] fullKey = new byte[24];
            System.arraycopy(tdesKey, 0, fullKey, 0, 16);
            System.arraycopy(tdesKey, 0, fullKey, 16, 8);
            tdesKey = fullKey;
        }
        SecretKeySpec keySpec = new SecretKeySpec(tdesKey, "DESede");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] encryptedData = cipher.doFinal(plaintext);
        return base64Encode(encryptedData);
    }

    public static String decrypt(String base64Input) throws Exception {
        byte[] encryptedData = base64Decode(base64Input);
        if (encryptedData == null || encryptedData.length == 0) {
            return null;
        }

        byte[] iv = hexToBytes(ivFixed);
        byte[] tdesKey = hexToBytes(mkFixed);
        if (tdesKey.length == 16) {
            byte[] fullKey = new byte[24];
            System.arraycopy(tdesKey, 0, fullKey, 0, 16);
            System.arraycopy(tdesKey, 0, fullKey, 16, 8);
            tdesKey = fullKey;
        }
        SecretKeySpec keySpec = new SecretKeySpec(tdesKey, "DESede");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] decryptedData = cipher.doFinal(encryptedData);
        String decryptedString = new String(decryptedData, StandardCharsets.UTF_8);
        if (decryptedString.length() < 2) {
            return null;
        }
        int checkSumIndex = decryptedString.length() - 2;
        String givenSum = decryptedString.substring(checkSumIndex);
        String mainData = decryptedString.substring(0, checkSumIndex);
        byte checkSum = xorChecksum(mainData.getBytes(StandardCharsets.UTF_8));
        if (givenSum.equalsIgnoreCase(String.format("%02X", checkSum & 0xFF))) {
            return decryptedString.toUpperCase();
        } else {
            return null;
        }
    }

    public static String base64Encode(byte[] data) {
        try {
            return Base64.encodeToString(data, Base64.NO_WRAP);
        } catch (Throwable t) {
            return java.util.Base64.getEncoder().encodeToString(data);
        }
    }

    public static byte[] base64Decode(String str) {
        try {
            return Base64.decode(str, Base64.NO_WRAP);
        } catch (Throwable t) {
            return java.util.Base64.getDecoder().decode(str);
        }
    }

    public static byte xorChecksum(byte[] data) {
        byte checksum = 0;
        for (byte b : data) {
            checksum ^= b;
        }
        return checksum;
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    public static String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
