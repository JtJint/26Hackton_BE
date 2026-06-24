package com.interviewhelper.account;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.springframework.stereotype.Component;

@Component
public class PasswordHasher {

	private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
	private static final int ITERATIONS = 120_000;
	private static final int KEY_LENGTH = 256;
	private final SecureRandom secureRandom = new SecureRandom();

	public String hash(String password) {
		byte[] salt = new byte[16];
		secureRandom.nextBytes(salt);
		byte[] hash = pbkdf2(password.toCharArray(), salt);
		return ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
	}

	public boolean matches(String password, String storedHash) {
		String[] parts = storedHash.split(":");
		if (parts.length != 3) {
			return false;
		}
		byte[] salt = Base64.getDecoder().decode(parts[1]);
		byte[] expected = Base64.getDecoder().decode(parts[2]);
		byte[] actual = pbkdf2(password.toCharArray(), salt);
		return java.security.MessageDigest.isEqual(expected, actual);
	}

	private byte[] pbkdf2(char[] password, byte[] salt) {
		try {
			PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
			return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
		}
		catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
			throw new IllegalStateException("Password hashing failed", exception);
		}
	}
}
