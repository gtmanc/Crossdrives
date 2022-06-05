package com.crossdrives.cdfs.allocation;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

public class IDProducer {

    public static String deriveID(Collection<String> ids){
        StringBuffer hexString = new StringBuffer();
        String concatenated = ids.stream().reduce(new String(), (prev, curr)->{
            prev = prev.concat(curr);
            return prev;
        });

        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("SHA-1");
            digest.update(concatenated.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            for (int i=0; i<messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
        } catch (NoSuchAlgorithmException e) {
            hexString.append("");
        }

        return hexString.toString();
    }
}
