package controlador;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class GestorContraseñas {
    
    // Configuración recomendada
    private static final int ITERATIONS = 65536;  // número de iteraciones
    private static final int KEY_LENGTH = 256;    // tamaño en bits
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    // Generar sal aleatoria segura
    public static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }

    // Generar hash PBKDF2
    public static String hashPassword(String password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
        byte[] hash = factory.generateSecret(spec).getEncoded();

        // Devolvemos hash y salt codificados en Base64 (para guardar fácilmente)
        return Base64.getEncoder().encodeToString(hash);
    }

}
