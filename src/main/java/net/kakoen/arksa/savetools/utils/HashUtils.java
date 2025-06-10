package net.kakoen.arksa.savetools.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Function;

public class HashUtils {

    public static Function<byte[], byte[]> defaultJvmHashAlgorithm(String algorithmName) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance(algorithmName);
        return (bytes) -> {
            messageDigest.reset();
            return messageDigest.digest(bytes);
        };
    }

}
