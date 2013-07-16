/**
 * AESEncryption.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2013 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.common.security;

import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.util.Base64;

/**
 * This class/interface
 * 
 * @author Jegadeesan
 * @Since Feb 22, 2013
 */
public class AESEncryption {

    public static String encrypt(byte[] key, String text) throws Exception {
        AlgorithmParameterSpec paramSpec = new IvParameterSpec(key);
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        Cipher ecipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        ecipher.init(Cipher.ENCRYPT_MODE, skeySpec, paramSpec);
        byte[] dstBuff = ecipher.doFinal(text.getBytes());
        return Base64.encodeToString(dstBuff, Base64.DEFAULT);
    }

    public static String decrypt(byte[] key, String data) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        Cipher decodeCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        AlgorithmParameterSpec paramSpec = new IvParameterSpec(key);
        decodeCipher.init(Cipher.DECRYPT_MODE, skeySpec, paramSpec);
        byte[] dstBuff = decodeCipher.doFinal(Base64.decode(data.getBytes(), Base64.DEFAULT));
        return new String(dstBuff);
    }

    public static byte[] getRawKey(byte[] seed) throws Exception {
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        sr.setSeed(seed);
        kgen.init(128, sr);
        SecretKey skey = kgen.generateKey();
        byte[] raw = skey.getEncoded();
        return raw;
    }

}
