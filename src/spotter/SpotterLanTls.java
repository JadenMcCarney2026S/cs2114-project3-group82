package spotter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Collections;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

final class SpotterLanTls
{
    private SpotterLanTls()
    {
    }

    static SSLContext serverContext(Path keystore, char[] password)
        throws IOException, GeneralSecurityException
    {
        KeyStore keys = KeyStore.getInstance("PKCS12");
        try (var input = Files.newInputStream(keystore))
        {
            keys.load(input, password);
        }
        boolean hasServerKey = false;
        for (String alias : Collections.list(keys.aliases()))
        {
            if (keys.entryInstanceOf(alias, KeyStore.PrivateKeyEntry.class))
            {
                hasServerKey = true;
                break;
            }
        }
        if (!hasServerKey)
        {
            throw new GeneralSecurityException("Host keystore must contain a private key.");
        }
        KeyManagerFactory managers =
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        managers.init(keys, password);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(managers.getKeyManagers(), null, null);
        return context;
    }
}
