package jp.syoboi.android.oauth;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

public class MyURLEncoder {

	static final String digits = "0123456789ABCDEF";


    public static CharSequence encode(CharSequence s, Charset charset) {
        if (s == null || charset == null) {
            throw new NullPointerException();
        }

        // Guess a bit bigger for encoded form
        StringBuilder buf = new StringBuilder(s.length() + 16);
        int start = -1;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9') || ".-*_".indexOf(ch) > -1) {
                if (start >= 0) {
                    convert(CharBuffer.wrap(s, start, i), buf, charset);
                    start = -1;
                }
//                if (ch != ' ') {
                    buf.append(ch);
//                } else {
//                    buf.append('+');
//                }
            } else {
                if (start < 0) {
                    start = i;
                }
            }
        }
        if (start >= 0) {
            convert(CharBuffer.wrap(s, start, s.length()), buf, charset);
        }
        return buf;
    }

    private static void convert(CharBuffer s, StringBuilder buf, Charset charset) {
    	ByteBuffer bb = charset.encode(s);
    	while (bb.hasRemaining()) {
    		byte b = bb.get();
            buf.append('%');
            buf.append(digits.charAt((b & 0xf0) >> 4));
            buf.append(digits.charAt(b & 0xf));
        }
    }
}
