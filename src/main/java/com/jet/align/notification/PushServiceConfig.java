package com.jet.align.notification;

import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.GeneralSecurityException;
import java.security.Security;

@Configuration
public class PushServiceConfig {

    @Bean
    public PushService pushService(
            @Value("${align.push.vapid.public-key}") String publicKey,
            @Value("${align.push.vapid.private-key}") String privateKey,
            @Value("${align.push.vapid.subject}") String subject
    ) throws GeneralSecurityException {
        Security.addProvider(new BouncyCastleProvider());
        return new PushService(publicKey, privateKey, subject);
    }
}
