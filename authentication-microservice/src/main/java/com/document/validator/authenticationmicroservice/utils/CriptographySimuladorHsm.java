package com.document.validator.authenticationmicroservice.utils;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    Logger logger = LogManager.getLogger(getClass());

    public void addKey(String valor){

        byte[] valorByte = valor.getBytes();

        key = new SecretKeySpec(Arrays.copyOf(valorByte,keysize),algoritmo);
    }

    public CriptographySimuladorHsm(){
        logger.debug("Entro a encriptar");
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
            logger.error( ex.getMessage() );
        } catch (NoSuchPaddingException ex) {
            logger.error( ex.getMessage() );
        } catch (InvalidKeyException ex) {
            logger.error( ex.getMessage() );
        } catch (IllegalBlockSizeException ex) {
            logger.error( ex.getMessage() );
        } catch (BadPaddingException ex) {
            logger.error( ex.getMessage() );
        }

        valorEncriptado = valorEncriptado.replace("+","0").replace("=","1").replace("*", "2").replace("/", "3");
        valorEncriptado = valorEncriptado.replace("\n","1").replace("\r","1");
        valorEncriptado = valorEncriptado.toUpperCase();
        return valorEncriptado;
    }
}
