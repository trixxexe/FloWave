
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class TestDec {
    public static void main(String[] args) throws Exception {
        String enc = "ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDycd7DeYRJD6QiS/Ul6+t730RBCdy3/A1wt85WvqXf7Uh5hKnruO6WGhw7tS9a8Gtq";
        byte[] keyBytes = "38588548".getBytes("UTF-8");
        SecretKeySpec key = new SecretKeySpec(keyBytes, "DES");
        Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] dec = cipher.doFinal(Base64.getDecoder().decode(enc));
        String url = new String(dec, "UTF-8").trim();
        System.out.println("Decrypted URL: " + url);
    }
}
