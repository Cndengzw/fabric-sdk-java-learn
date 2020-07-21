package cn.com.fabric.sdk;

import org.hyperledger.fabric.sdk.Enrollment;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * @author Deng Zhiwen
 * @date 2020/7/20 10:57
 */
public class UserUtils {

    private static class CAEnrollment implements Enrollment {
        private PrivateKey key;     // 私钥
        private String ecert;       // 证书

        public CAEnrollment(PrivateKey key,String ecert) {
            this.key = key;
            this.ecert = ecert;
        }

        @Override
        public PrivateKey getKey() {
            return key;
        }

        @Override
        public String getCert() {
            return ecert;
        }
    }

    public static Enrollment getEnrollment(String keyFolderPah, String keyFileName, String certFolderPath, String certFileName)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        PrivateKey key = null;
        String certificate = null;

        InputStream isKey = null;
        BufferedReader brKey = null;

        try {
            isKey = new FileInputStream(keyFolderPah + File.separator + keyFileName);
            brKey = new BufferedReader(new InputStreamReader(isKey));
            StringBuilder keyBuilder = new StringBuilder();

            for (String line = brKey.readLine(); line != null; line = brKey.readLine()) {
                if (line.indexOf("PRIVATE") == -1) {
                    keyBuilder.append(line);
                }
            }

            certificate = new String(Files.readAllBytes(Paths.get(certFolderPath, certFileName)));

            byte[] encoded = DatatypeConverter.parseBase64Binary(keyBuilder.toString());
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            KeyFactory kf = KeyFactory.getInstance("ECDSA");
            key = kf.generatePrivate(keySpec);
        } finally {
            isKey.close();
            brKey.close();
        }
        return new CAEnrollment(key, certificate);

    }

}
