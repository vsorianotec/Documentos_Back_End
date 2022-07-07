package com.document.validator.documentsmicroservice.service;

import com.document.validator.documentsmicroservice.dto.SingResponseDTO;
import com.document.validator.documentsmicroservice.dto.ValidateResponseDTO;
import com.document.validator.documentsmicroservice.entity.Document;
import com.document.validator.documentsmicroservice.entity.User;
import com.document.validator.documentsmicroservice.repository.UserRepository;
import com.document.validator.documentsmicroservice.repository.DocumentRepository;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Date;
import java.util.UUID;
import org.apache.commons.io.FilenameUtils;

@Service
public class DocumentService {

    @Autowired
    DocumentRepository documentRepository;
    @Autowired
    UserRepository userRepository;

    public String uploadDir = "/tmp";
    //public String uploadDir = "C:\\Temporal";



    public SingResponseDTO sign(MultipartFile file, String description, int userId){
        SingResponseDTO responseDTO=new SingResponseDTO();
        try {
            String uuid = UUID.randomUUID().toString();
            String fileName= uuid + "."+ FilenameUtils.getExtension(StringUtils.cleanPath(file.getOriginalFilename()));
            String rutaArchivoFirmado= uploadDir + File.separator + fileName;

            Path copyLocation = Paths
                    .get(rutaArchivoFirmado);
            Files.copy(file.getInputStream(), copyLocation, StandardCopyOption.REPLACE_EXISTING);

            Document document = new Document();
            document.setFileName(StringUtils.cleanPath(file.getOriginalFilename()));
            document.setDescription(description);
            document.setCreatedBy(userId);
            document.setCreatedDate(new Date());
            document.setHashOriginalDocument(generaHash(rutaArchivoFirmado));
            document = documentRepository.save(document);

            FileWriter fichero = null;
            PrintWriter pw = null;
            Gson gson = new Gson();
            try
            {
                fichero = new FileWriter(rutaArchivoFirmado,true);
                pw = new PrintWriter(fichero);

                pw.print("--SOS" + gson.toJson(document) + "EOS--");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    // Nuevamente aprovechamos el finally para
                    // asegurarnos que se cierra el fichero.
                    if (null != fichero)
                        fichero.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }

            document.setHashSignedDocument(generaHash(rutaArchivoFirmado));
            documentRepository.save(document);

            responseDTO.setFileName(fileName);
            responseDTO.setStatus(0);
            responseDTO.setCodeError("DOCU000");
            responseDTO.setMsgError("OK");
        } catch (Exception e) {
            e.printStackTrace();
            responseDTO.setStatus(0);
            responseDTO.setCodeError("INTERNAL");
            responseDTO.setMsgError("No se pudo guardar el archivo " + file.getOriginalFilename() + ". ¡Prueba Nuevamente!.  Exception: " + e.getMessage());
        }
        return responseDTO;
    }

    public ValidateResponseDTO validate(MultipartFile file){
        ValidateResponseDTO responseDTO=new ValidateResponseDTO();
        try {

            String uuid = UUID.randomUUID().toString();
            String fileName= uuid + "."+ FilenameUtils.getExtension(StringUtils.cleanPath(file.getOriginalFilename()));
            String rutaArchivoFirmado= uploadDir + File.separator + fileName;
            Path copyLocation = Paths
                    .get(rutaArchivoFirmado);
            Files.copy(file.getInputStream(), copyLocation, StandardCopyOption.REPLACE_EXISTING);

            FileReader archivo = null;
            InputStreamReader fr = null;
            BufferedReader br = null;
            Document documentBD = null;
            User user= null;
            try {
                // Apertura del fichero y creacion de BufferedReader para poder
                // hacer una lectura comoda (disponer del metodo readLine()).
                archivo = new FileReader(rutaArchivoFirmado);
                br= new BufferedReader(archivo);

                // Lectura del fichero
                String linea;
                String lastLine="";
                while((linea=br.readLine())!=null) {
                    //System.out.println(linea);
                    lastLine=linea;
                }


                archivo.close();

                int inicio = lastLine.indexOf("--SOS");
                int fin = lastLine.indexOf("EOS--");

                if(inicio<0 || fin<0){
                    responseDTO.setStatus(1);
                    responseDTO.setCodeError("DOCU001");
                    responseDTO.setMsgError("El documento no contiene la firma");
                    return responseDTO;
                }

                System.out.println("inicio: " + inicio);
                System.out.println("fin: " + fin);
                String json=lastLine.substring(inicio + 5,fin);
                System.out.println("json: " + json );

                Gson gson = new Gson();
                Document document = gson.fromJson(json,Document.class);
                if(document==null){
                    responseDTO.setStatus(1);
                    responseDTO.setCodeError("DOCU002");
                    responseDTO.setMsgError("El documento contiene la firma pero fue modificado");
                    return responseDTO;
                }else{
                    documentBD = documentRepository.findById(document.getId()).orElse(null);
                    if(documentBD!=null) {
                        user = userRepository.getReferenceById(document.getCreatedBy());
                        if (!documentBD.getHashSignedDocument().equals(generaHash(rutaArchivoFirmado))) {
                            responseDTO.setDocumentId(documentBD.getId());
                            responseDTO.setCreatedDate(documentBD.getCreatedDate());
                            responseDTO.setOriginalName(documentBD.getFileName());
                            responseDTO.setAuthor(user.getName());
                            responseDTO.setEmail(user.getEmail());
                            responseDTO.setStatus(1);
                            responseDTO.setCodeError("DOCU002");
                            responseDTO.setMsgError("El documento contiene la firma pero fue modificado");
                            return responseDTO;
                        }
                    }else{
                        responseDTO.setStatus(1);
                        responseDTO.setCodeError("DOCU003");
                        responseDTO.setMsgError("El documento contiene una firma no reconocida");
                        return responseDTO;
                    }
                }

            }catch(Exception e){
                e.printStackTrace();
                responseDTO.setStatus(0);
                responseDTO.setCodeError("INTERNAL");
                responseDTO.setMsgError("Could not store file " + file.getOriginalFilename() + ". Please try again!. Exception: " + e.getMessage());
                return responseDTO;
            }finally{
                // En el finally cerramos el fichero, para asegurarnos
                // que se cierra tanto si todo va bien como si salta
                // una excepcion.
                try{
                    if( archivo != null )
                        archivo.close();

                }catch (Exception e2){
                    e2.printStackTrace();
                }
            }

            Files.delete(copyLocation);

            //responseDTO.setDocument(documentBD);
            //responseDTO.setUser(user);
            responseDTO.setDocumentId(documentBD.getId());
            responseDTO.setCreatedDate(documentBD.getCreatedDate());
            responseDTO.setOriginalName(documentBD.getFileName());
            responseDTO.setAuthor(user.getName());
            responseDTO.setEmail(user.getEmail());

            responseDTO.setStatus(0);
            responseDTO.setCodeError("DOCU000");
            responseDTO.setMsgError("OK");
        } catch (Exception e) {
            e.printStackTrace();
            responseDTO.setStatus(0);
            responseDTO.setCodeError("INTERNAL");
            responseDTO.setMsgError("Could not store file " + file.getOriginalFilename() + ". Please try again!. Exception: " + e.getMessage());
        }
        return responseDTO;
    }

    public static String generaHash(String rutaArchivo)
    {
        String hash = "";
        //declarar funcion de resumen
        try{
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256"); // Inicializa con algoritmo seleccionado

            //leer fichero byte a byte
            try{
                InputStream archivo = new FileInputStream( rutaArchivo );
                byte[] buffer = new byte[1];
                int fin_archivo = -1;
                int caracter;

                caracter = archivo.read(buffer);

                while( caracter != fin_archivo ) {

                    messageDigest.update(buffer); // Pasa texto claro a la función resumen
                    caracter = archivo.read(buffer);
                }

                archivo.close();//cerramos el archivo
                byte[] resumen = messageDigest.digest(); // Genera el resumen

                //Pasar los resumenes a hexadecimal

                for (int i = 0; i < resumen.length; i++)
                {
                    hash += Integer.toHexString((resumen[i] >> 4) & 0xf);
                    hash += Integer.toHexString(resumen[i] & 0xf);
                }
            }
            //lectura de los datos del fichero
            catch(java.io.FileNotFoundException fnfe) {
                //manejar excepcion archivo no encontrado
            }
            catch(java.io.IOException ioe) {
                //manejar excepcion archivo
            }

        }
        //declarar funciones resumen
        catch(java.security.NoSuchAlgorithmException nsae) {
            //manejar excepcion algorito seleccionado erroneo
        }
        return hash;//regresamos el resumen

    }

    public void download(String fileName,HttpServletResponse response){
        try {
            File file = new File(uploadDir + File.separator + fileName);
            if (file.exists()) {

                //get the mimetype
                String mimeType = URLConnection.guessContentTypeFromName(file.getName());
                if (mimeType == null) {
                    //unknown mimetype so set the mimetype to application/octet-stream
                    mimeType = "application/octet-stream";
                }

                response.setContentType(mimeType);

                /**
                 * In a regular HTTP response, the Content-Disposition response header is a
                 * header indicating if the content is expected to be displayed inline in the
                 * browser, that is, as a Web page or as part of a Web page, or as an
                 * attachment, that is downloaded and saved locally.
                 *
                 */

                /**
                 * Here we have mentioned it to show inline
                 */
                response.setHeader("Content-Disposition", String.format("inline; filename=\"" + file.getName() + "\""));

                //Here we have mentioned it to show as attachment
                //response.setHeader("Content-Disposition", String.format("attachment; filename=\"" + file.getName() + "\""));

                response.setContentLength((int) file.length());

                InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

                FileCopyUtils.copy(inputStream, response.getOutputStream());

                file.delete();

            }
        }catch (Exception e){
            response.setStatus(400);
        }
    }
}
