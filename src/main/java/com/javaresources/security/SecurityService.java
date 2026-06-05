package com.javaresources.security;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class SecurityService {

    private static final Logger log = LoggerFactory.getLogger(SecurityService.class);
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern SAFE_INPUT =
        Pattern.compile("^[a-zA-Z0-9\\s\\-_.,áéíóúÁÉÍÓÚñÑ]+$");
    private static final String HMAC_KEY = "java-resources-hmac-key-2024";

    @SuppressWarnings("deprecation")
    public Map<String, Object> hashDemo(String input) {
        Map<String, Object> result = new LinkedHashMap<>();
        String guavaHash   = Hashing.sha256().hashString(input, StandardCharsets.UTF_8).toString();
        String commonsHash = DigestUtils.sha256Hex(input);
        result.put("input",         input.length() + " caracteres");
        result.put("sha256_guava",  guavaHash);
        result.put("sha256_commons",commonsHash);
        result.put("match",         guavaHash.equals(commonsHash));
        log.info("Hash SHA-256 generado para input de {} chars", input.length());
        return result;
    }

    @SuppressWarnings("deprecation")
    public Map<String, Object> hmacDemo(String data) {
        Map<String, Object> result = new LinkedHashMap<>();
        HashCode hmac = Hashing.hmacSha256(HMAC_KEY.getBytes(StandardCharsets.UTF_8))
            .hashString(data, StandardCharsets.UTF_8);
        String signature = hmac.toString();
        boolean valid = verifyHmac(data, signature);
        result.put("data",      data);
        result.put("signature", signature);
        result.put("verified",  valid);
        log.info("HMAC generado y verificado: {}", valid);
        return result;
    }

    @SuppressWarnings("deprecation")
    public boolean verifyHmac(String data, String expected) {
        String actual = Hashing.hmacSha256(HMAC_KEY.getBytes(StandardCharsets.UTF_8))
            .hashString(data, StandardCharsets.UTF_8).toString();
        byte[] a = actual.getBytes(StandardCharsets.UTF_8);
        byte[] b = expected.getBytes(StandardCharsets.UTF_8);
        if (a.length != b.length) return false;
        int r = 0; for (int i = 0; i < a.length; i++) r |= a[i] ^ b[i];
        return r == 0;
    }

    public Map<String, Object> base64Demo(String input) {
        Map<String, Object> result = new LinkedHashMap<>();
        String encoded = new String(Base64.encodeBase64(input.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        String decoded = new String(Base64.decodeBase64(encoded), StandardCharsets.UTF_8);
        result.put("original", input);
        result.put("encoded",  encoded);
        result.put("decoded",  decoded);
        result.put("roundtrip_ok", input.equals(decoded));
        return result;
    }

    public Map<String, Object> tokenDemo(int length) {
        if (length < 8 || length > 128) length = 32;
        String token = RandomStringUtils.randomAlphanumeric(length);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token",   token);
        result.put("length",  token.length());
        result.put("preview", token.substring(0, 6) + "...");
        log.info("Token seguro generado de {} chars", length);
        return result;
    }

    public Map<String, Object> validateDemo(String email, String input) {
        Map<String, Object> result = new LinkedHashMap<>();
        boolean emailOk = StringUtils.isNotBlank(email) && EMAIL_PATTERN.matcher(email.trim()).matches();
        boolean inputOk = StringUtils.isNotBlank(input) && SAFE_INPUT.matcher(input).matches();
        result.put("email",       email);
        result.put("email_valid", emailOk);
        result.put("input",       input);
        result.put("input_safe",  inputOk);
        result.put("masked_email", emailOk ? maskEmail(email) : "N/A");
        if (!inputOk) log.warn("SEGURIDAD: entrada peligrosa detectada len={}", input.length());
        return result;
    }

    private String maskEmail(String email) {
        if (!email.contains("@")) return "***";
        String[] p = email.split("@");
        String u = p[0];
        String m = u.length() <= 2 ? "**"
            : u.charAt(0) + StringUtils.repeat("*", u.length() - 2) + u.charAt(u.length() - 1);
        return m + "@" + p[1];
    }
}
