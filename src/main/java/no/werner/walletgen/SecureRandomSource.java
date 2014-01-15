package no.werner.walletgen;

import at.archistar.crypto.random.RandomSource;

import java.security.SecureRandom;

/**
 * Created by werner on 04/01/14.
 */
public class SecureRandomSource implements RandomSource {
    private SecureRandom secureRandom = new SecureRandom();

    @Override
    public int generateByte() {
        return secureRandom.nextInt(255) + 1;
    }
}
