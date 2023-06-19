package com.document.validator.documentsmicroservice.service;

import com.document.validator.documentsmicroservice.dto.CompareMinisResponseDTO;
import com.document.validator.documentsmicroservice.dto.SingResponseDTO;
import com.document.validator.documentsmicroservice.dto.ValidateResponseDTO;
import com.document.validator.documentsmicroservice.dto.VerifyImageQrResponseDTO;
import com.document.validator.documentsmicroservice.entity.Document;
import com.document.validator.documentsmicroservice.entity.User;
import com.document.validator.documentsmicroservice.repository.UserRepository;
import com.document.validator.documentsmicroservice.repository.DocumentRepository;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.opencv.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import org.apache.commons.io.FilenameUtils;

@Service
public class DocumentService {

    @Autowired
    DocumentRepository documentRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    FileService fileService;

    public static String uploadDir = "C:\\Temporal";
    public static String uuid;
    private static String fileext;
    private static double acceptancePercentage = 99.69;
    private static double similarityPercentage = 98.9;


    public SingResponseDTO sign(MultipartFile file, String description, int userId){
        SingResponseDTO responseDTO=new SingResponseDTO();
        try {

            System.out.println("OpenCV Version: " + Core.VERSION);
            System.out.println("|Sign|Ini."+LocalDateTime.now());

            uuid = UUID.randomUUID().toString();

            fileext = fileService.changeFileExtension(FilenameUtils.getExtension(StringUtils.cleanPath(file.getOriginalFilename())).toLowerCase());

            String fileName= uuid + "."+ fileext; //FilenameUtils.getExtension(StringUtils.cleanPath(file.getOriginalFilename()));
            String rutaArchivoFirmado= uploadDir + File.separator + "ImgSealed" + File.separator + fileName;
            String rutaArchivoOriginal=uploadDir + File.separator + "Img" + File.separator + file.getOriginalFilename();
            String rutaArchivoOriginalCompress= uploadDir + File.separator + "Img"+ File.separator + "c_"+file.getOriginalFilename();

            Path copyLocation = Paths.get(rutaArchivoFirmado);
            Path copyLocationOri = Paths.get(rutaArchivoOriginal);
            Files.copy(file.getInputStream(), copyLocation, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(file.getInputStream(), copyLocationOri,StandardCopyOption.REPLACE_EXISTING);

            Document document = new Document();
            document.setFileName(StringUtils.cleanPath(file.getOriginalFilename()));
            document.setDescription(description);
            document.setUuid(uuid);
            document.setCreatedBy(userId);
            document.setCreatedDate(new Date());
            document.setHashOriginalDocument(fileService.generateHash(rutaArchivoOriginal));
            document = documentRepository.save(document);

            if(fileService.isStaticImage(fileext)){
                fileService.generateCompressImage(rutaArchivoOriginal,rutaArchivoOriginalCompress);
                fileService.generateThumbnail(rutaArchivoOriginal,uploadDir + File.separator + "Miniaturas"+ File.separator+"m_"+uuid+".jpg");
                fileService.sealImage(rutaArchivoOriginal,rutaArchivoFirmado,document);
            }else {
                fileService.sealFile(rutaArchivoOriginal,rutaArchivoFirmado,document);
            }
            document.setHashSignedDocument(fileService.generateHash(rutaArchivoFirmado));
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
        System.out.println("|Sign|Fin."+LocalDateTime.now());
        return responseDTO;
    }

    public ValidateResponseDTO validate(MultipartFile file){
        ValidateResponseDTO responseDTO=new ValidateResponseDTO();
        Gson gson = new Gson();
        Path copyLocation = null;

        try {
            System.out.println("|validate|Ini."+LocalDateTime.now());
            String uuid = UUID.randomUUID().toString();
            String fileName= uuid + "."+ FilenameUtils.getExtension(StringUtils.cleanPath(file.getOriginalFilename()));
            String rutaArchivoFirmado= uploadDir + File.separator + "tmp"+File.separator+"Sellado"+fileName;
            copyLocation = Paths.get(rutaArchivoFirmado);
            Files.copy(file.getInputStream(), copyLocation, StandardCopyOption.REPLACE_EXISTING);
            fileext = fileService.changeFileExtension(FilenameUtils.getExtension(StringUtils.cleanPath(file.getOriginalFilename())).toLowerCase());
            System.out.println(fileext);

            String seal="";
            Document document  = new Document();
            if(fileService.isStaticImage(fileext)){
                seal= verifiyImageQr(rutaArchivoFirmado);
                System.out.println("Response QR:<"+seal+"> Length:"+seal.length());
                if (!seal.equals("")) { // Exists QR
                    System.out.println("QR=<"+seal+">");
                    String[] parts = seal.split("\\|"); //Result convert in matrix and set in document object
                    if(parts.length>0) {
                        document.setId(Integer.parseInt(parts[0]));
                        document.setHashOriginalDocument(parts[1]);
                        document.setCreatedBy(Integer.parseInt(parts[3]));
                    }
                    Document documentBD = documentRepository.findById(document.getId()).orElse(null);
                    if (documentBD!=null ) {
                        System.out.println("<4>Find with OpenCV . "+ LocalDateTime.now());
                        System.out.println("Begin comparing...");
                        String rutaArchivoQR = uploadDir +File.separator +"ImgSealed"+File.separator+documentBD.getUuid() + ".jpg";
                        // String rutaArchivoDiff= uploadDir +File.separator+"tmp"+File.separator+uuid+"_diff.jpg";
                        // String rutaArchivoDiffere = uploadDir + File.separator+"tmp" +File.separator+ uuid + "_differe.jpg";
                        String rutaArchivoFakeSize = uploadDir+File.separator+"tmp"+File.separator+"fake_size.jpg";
                        String rutaArchivoDiffereFake = uploadDir + File.separator+"tmp"+File.separator+ uuid + "_differeFake.jpg";
                        double resCompare= fileService.compareContentQR(rutaArchivoQR,rutaArchivoFirmado,rutaArchivoFakeSize,rutaArchivoDiffereFake);
                        double matchPorcentage=100 - resCompare;
                        System.out.println("matchPorcentage: " + matchPorcentage);
                        if(matchPorcentage>99.99){
                            User user = userRepository.getReferenceById(documentBD.getCreatedBy());
                            System.out.println("File signed: "+rutaArchivoFirmado);

                            responseDTO.setStatus(0);
                            responseDTO.setCodeError("DOCU000");
                            responseDTO.setMsgError("OK");
                            responseDTO.setDocumentId(documentBD.getId());
                            responseDTO.setCreatedDate(documentBD.getCreatedDate());
                            responseDTO.setOriginalName(documentBD.getFileName());
                            responseDTO.setAuthor(user.getName());
                            responseDTO.setEmail(user.getEmail());
                            System.out.println("|Val|ConQROK."+LocalDateTime.now());
                            // 2% umbral corrección
                        } else if (matchPorcentage>98) {
                            responseDTO.setStatus(1);
                            responseDTO.setCodeError("DOCU004");
                            responseDTO.setMsgError("Not an Alipsé Sealed File, yet it looks VERY MUCH LIKE\none of the images in our database by [author] (please be warned it’s not identical) "); //El documento contiene una firma no reconocida
                            responseDTO.setFileName(uuid + "_differeFake.jpg");

                            System.out.println("|Val|SinQRcomparingFinFake."+LocalDateTime.now());
                            return responseDTO;
                        } else {
                            responseDTO.setStatus(1);
                            responseDTO.setCodeError("DOCU004");
                            responseDTO.setMsgError("Not an Alipsé Sealed File, we cannot determine its authenticity"); //El documento contiene una firma no reconocida
                            responseDTO.setFileName(uuid + "_differeFake.jpg");

                            System.out.println("|Val|SinQRComparingNotSaledFinFake."+LocalDateTime.now());
                            return responseDTO;
                        }
                    }else{
                        responseDTO.setStatus(1);
                        responseDTO.setCodeError("DOCU002");
                        responseDTO.setMsgError("Alipsé Sealed FAKE file\n[the author] invests to avoid impersonation");
                        responseDTO.setFileName("fake.jpg");

                        System.out.println("|Val|SinQRNotSaledFinFake."+LocalDateTime.now());
                        return responseDTO;
                    }
                }else{
                    String rutaMiniatura= uploadDir + File.separator + "Miniaturas"+ File.separator+"mtmp_"+fileName;
                    fileService.generateThumbnail(rutaArchivoFirmado,rutaMiniatura);
                    CompareMinisResponseDTO compareMinisResponseDTO = compareMinis(rutaMiniatura);
                    Files.delete(Paths.get(rutaMiniatura));
                    if(compareMinisResponseDTO.getMatchPercentage()>=acceptancePercentage){
                        responseDTO.setDocumentId(compareMinisResponseDTO.getDocument().getId());
                        responseDTO.setCreatedDate(compareMinisResponseDTO.getDocument().getCreatedDate());
                        responseDTO.setOriginalName(compareMinisResponseDTO.getDocument().getFileName());
                        responseDTO.setAuthor(compareMinisResponseDTO.getNameAuthor());
                        responseDTO.setEmail(compareMinisResponseDTO.getEmailAuthor());

                        responseDTO.setStatus(0);
                        responseDTO.setCodeError("DOCU000");
                        responseDTO.setMsgError("OK");

                        System.out.println("|Val|FinMiniOK."+LocalDateTime.now());
                        return responseDTO;
                    } else if (compareMinisResponseDTO.getMatchPercentage()>similarityPercentage) {
                        responseDTO.setStatus(1);
                        responseDTO.setCodeError("DOCU004");
                        responseDTO.setMsgError("Not an Alipsé Sealed File, yet it looks VERY MUCH LIKE\n one of the images in our database by [author] (please be warned it’s not identical)"); //El documento contiene una firma no reconocida
                        responseDTO.setFileName("fake.jpg");

                        System.out.println("|Val|FinMiniLike."+LocalDateTime.now());
                        return responseDTO;
                    } else {
                        responseDTO.setStatus(1);
                        responseDTO.setCodeError("DOCU004");
                        responseDTO.setMsgError("Alipsé Sealed FAKE file\n [the author] invests to avoid impersonation"); //El documento contiene una firma no reconocida
                        responseDTO.setFileName("fake.jpg");

                        System.out.println("|Val|FinMiniFake."+LocalDateTime.now());
                        return responseDTO;
                    }
                }
            }else{
                seal=fileService.getSeal(rutaArchivoFirmado);
                if(!seal.equals("")){
                    System.out.println("json: " + seal);
                    //byte[] decodedBytes = Base64.decodeBase64(seal);
                    //System.out.println("decodedBytes " + new String(decodedBytes));
                    //json=decodedBytes.toString();
                    document = gson.fromJson(seal, Document.class);
                    Document documentBD = documentRepository.findById(document.getId()).orElse(null);
                    if (documentBD==null || !documentBD.getHashSignedDocument().equals(fileService.generateHash(rutaArchivoFirmado))) {
                        responseDTO.setStatus(1);
                        responseDTO.setCodeError("DOCU002");
                        responseDTO.setMsgError("Not an Alipsé Sealed File, yet it looks VERY MUCH LIKE\n one of the images in our database by [author] (please be warned it’s not identical)");
                        responseDTO.setFileName("fake.jpg");

                        System.out.println("|Val|SinQRFinFake."+LocalDateTime.now());
                        return responseDTO;
                    }else{
                        User user = userRepository.getReferenceById(documentBD.getCreatedBy());
                        System.out.println("File signed: "+rutaArchivoFirmado);

                        responseDTO.setStatus(0);
                        responseDTO.setCodeError("DOCU000");
                        responseDTO.setMsgError("OK");
                        responseDTO.setDocumentId(documentBD.getId());
                        responseDTO.setCreatedDate(documentBD.getCreatedDate());
                        responseDTO.setOriginalName(documentBD.getFileName());
                        responseDTO.setAuthor(user.getName());
                        responseDTO.setEmail(user.getEmail());
                        System.out.println("|Val|SinQROK."+LocalDateTime.now());
                    }
                }else{
                    responseDTO.setStatus(1);
                    responseDTO.setCodeError("DOCU001");
                    responseDTO.setMsgError("Alipsé Sealed FAKE file\n[the author] invests to avoid impersonation");
                    responseDTO.setFileName("fake.jpg");

                    System.out.println("|Val|FinSinQRFake."+LocalDateTime.now());
                    return responseDTO;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            responseDTO.setStatus(0);
            responseDTO.setCodeError("INTERNAL");
            responseDTO.setMsgError("Could not store file " + file.getOriginalFilename() + ". Please try again!. Exception: " + e.getMessage());
        }finally {
            if(Files.exists(copyLocation)){
                try {
                    Files.delete(copyLocation);
                }catch (Exception e){
                    System.out.println("Error al eliminar archivo de trabajo");
                }
            }
        }
        System.out.println("|validate|FinOK."+LocalDateTime.now());
        return responseDTO;
    }

    private CompareMinisResponseDTO compareMinis(String rutaArchivoMiniUpload) throws IOException {
        System.out.println("Count repository: "+documentRepository.count());
        List<Document> documents = documentRepository.findAll();
        String rutaArchivoMini = "";
        Double minorDifference = 200.0,difference=200.0;
        Document documentBestMach = null;
        for(Document document : documents){
            rutaArchivoMini = uploadDir + File.separator + "Miniaturas"+ File.separator+"m_"+document.getUuid()+".jpg";
            System.out.print("Validando existencia: "+rutaArchivoMini);
            File fileMiniInBD = new File(rutaArchivoMini);
            // Checking if the specified file exists or not
            if (fileMiniInBD.exists()) {
                System.out.println(" Exists");
                difference = fileService.compareMinisProcess(rutaArchivoMiniUpload,rutaArchivoMini);
            }else{
                System.out.println(" Does not Exists");
            }
            System.out.println("difference: "+difference);
            System.out.println("minorDifference: "+minorDifference);
            if(difference<minorDifference){
                minorDifference = difference;
                documentBestMach=document;
                System.out.println("Menor diferencia ... : "+minorDifference);
            }
        }
        double bestMatch = 100 - minorDifference;
        System.out.println("Mejor coincidencia: "+bestMatch);
        CompareMinisResponseDTO response = new CompareMinisResponseDTO();

        if(bestMatch> acceptancePercentage ){
            System.out.println("Document of best match: " + documentBestMach);
            User author = userRepository.findById(documentBestMach.getId()).orElse(null);;
            if(author!=null){
                response.setEmailAuthor(author.getEmail());
                response.setNameAuthor(author.getName());
            }
        }else if(bestMatch >= similarityPercentage && bestMatch <= acceptancePercentage ){
            File source = new File(uploadDir + File.separator+"img"+ File.separator+"partialfake.jpg");
            File dest = new File(uploadDir + File.separator+"tmp"+File.separator+ uuid + "_differeFake.jpg");
            FileUtils.copyFile(source, dest);
        }else if(bestMatch < similarityPercentage){
            File source = new File(uploadDir + File.separator+"img"+File.separator+"fake.jpg");
            File dest = new File(uploadDir + File.separator+"tmp"+File.separator + uuid + "_differeFake.jpg");
            FileUtils.copyFile(source, dest);
        }

        response.setStatus(0);
        response.setCodeError("DOCU000");
        response.setMsgError("OK");
        response.setMatchPercentage(bestMatch);
        response.setDocument(documentBestMach);
        return response;
    }

    public String verifiyImageQr(String pathfile){
        String data="";
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();

            // Load a file from disk.
            Resource file1 = new FileSystemResource(pathfile);
            multipartBodyBuilder.part("file", file1, MediaType.IMAGE_JPEG);
            MultiValueMap<String, HttpEntity<?>> multipartBody = multipartBodyBuilder.build();
            HttpEntity<MultiValueMap<String, HttpEntity<?>>> httpEntity = new HttpEntity<>(multipartBody, headers);
            ResponseEntity<VerifyImageQrResponseDTO> responseEntity = restTemplate.postForEntity("http://localhost:8083/decodeqr/verifyImageQR", httpEntity,
                    VerifyImageQrResponseDTO.class);

            System.out.println(responseEntity.getBody().getData());
            data=responseEntity.getBody().getData();

        }catch(Exception e){
            System.out.println(e.getMessage());
        }
        return data;
    }

    public void download(String fileName,HttpServletResponse response){
        try {
            File file = null;
            if(fileName.equals("fake.jpg")){
                file = new File(uploadDir + File.separator + "img" + File.separator + "fake.jpg");
            }else if(fileName.contains("differeFake.jpg")){
                file = new File(uploadDir + File.separator + "tmp" + File.separator + fileName);
            }else{
                file = new File(uploadDir + File.separator + "ImgSealed" + File.separator + fileName);
            }
            if(file.exists()){
                //get the mimetype
                String mimeType = URLConnection.guessContentTypeFromName(file.getName());
                if (mimeType == null) {
                    //unknown mimetype so set the mimetype to application/octet-stream
                    mimeType = "application/octet-stream";
                }
                response.setContentType(mimeType);

                /**
                 * Here we have mentioned it to show inline
                 */
                System.out.println("filename: " + file.getName());
                response.setHeader("Content-Disposition", String.format("inline; filename=\"" + file.getName() + "\""));
                response.setContentLength((int) file.length());
                InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
                FileCopyUtils.copy(inputStream, response.getOutputStream());
            }

        }catch (Exception e){
            response.setStatus(400);
        }
    }
}
