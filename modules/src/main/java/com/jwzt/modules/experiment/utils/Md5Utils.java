package com.jwzt.modules.experiment.utils;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;

public class Md5Utils {

    private static final Logger log = LoggerFactory.getLogger(Md5Utils.class);

    public static String md5(String src, String salt) {
        String toEncode = src + ((salt == null) ? "" : ("{" + salt + "}"));
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
                'e', 'f'};
        try {
            byte[] btInput = toEncode.getBytes();
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            mdInst.update(btInput);
            byte[] md = mdInst.digest();
            int j = md.length;
            char[] str = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            log.error("md5 is error {}", ExceptionUtils.getStackTrace(e));
            return null;
        }
    }

    public static String md5(String s) {
        return md5(s, null);
    }
}
