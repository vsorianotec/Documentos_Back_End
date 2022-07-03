package com.document.validator.documentsmicroservice.utils;

import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Component;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

@Component
public class CriptographySimuladorHsm {

    private SecretKey key;
    private Cipher cipher;
    private String algoritmo= "AES";
    private int keysize=16;

    public void addKey(String valor){

        byte[] valorByte = valor.getBytes();

        key = new SecretKeySpec(Arrays.copyOf(valorByte,keysize),algoritmo);
    }

    public CriptographySimuladorHsm(){

        System.out.println("Entro a encriptar");
        addKey("67890234234d");
    }

    public String encriptar(String cadena){

        String valorEncriptado = "";

        try{

            cipher = Cipher.getInstance(algoritmo);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] cadenaByte = cadena.getBytes();
            byte[] cipherByte = cipher.doFinal(cadenaByte);

            valorEncriptado = new String(new Base64(true).encode(cipherByte));

        }catch (NoSuchAlgorithmException ex) {
            System.err.println( ex.getMessage() );
        } catch (NoSuchPaddingException ex) {
            System.err.println( ex.getMessage() );
        } catch (InvalidKeyException ex) {
            System.err.println( ex.getMessage() );
        } catch (IllegalBlockSizeException ex) {
            System.err.println( ex.getMessage() );
        } catch (BadPaddingException ex) {
            System.err.println( ex.getMessage() );
        }

        valorEncriptado = valorEncriptado.replace("+","0").replace("=","1").replace("*", "2").replace("/", "3");
        valorEncriptado = valorEncriptado.replace("\n","1").replace("\r","1");
        valorEncriptado = valorEncriptado.toUpperCase();
        return valorEncriptado;
    }
}
