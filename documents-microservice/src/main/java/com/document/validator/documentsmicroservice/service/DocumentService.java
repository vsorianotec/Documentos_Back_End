package com.document.validator.documentsmicroservice.service;

import com.document.validator.documentsmicroservice.dto.*;
import com.document.validator.documentsmicroservice.entity.Document;
import com.document.validator.documentsmicroservice.entity.User;
import com.document.validator.documentsmicroservice.repository.UserRepository;
import com.document.validator.documentsmicroservice.repository.DocumentRepository;
import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.workdir}")
    public String workdir;
    @Value("${app.acceptancePercentage}")
    private double acceptancePercentage;
    @Value("${app.similarityPercentage}")
    private double similarityPercentage;
    @Value("${app.decodeqr-microservice.domain}")
    private String decodeqrDomain;

    Gson gson = new Gson();
    Logger logger = LogManager.getLogger(getClass());

    public SingResponseDTO sign(MultipartFile file, String description, int userId){
        SingResponseDTO responseDTO=new SingResponseDTO();
        try {
            logger.info("Started sign:"+LocalDateTime.now());
            logger.info("OpenCV Version: " + Core.VERSION);

            String uuid = UUID.randomUUID().toString();
            String fileext = fileService.changeFileExtension(FilenameUtils.getExtension(StringUtils.cleanPath(file.getOriginalFilename())).toLowerCase());
            String fileName= uuid + "."+ fileext;
            String rutaArchivoFirmado= workdir + File.separator + "FilesSealed" + File.separator + fileName;
            String rutaArchivoOriginal= workdir + File.separator + "Files" + File.separator + file.getOriginalFilename();
            String rutaArchivoOriginalCompress= workdir + File.separator + "Files"+ File.separator + "c_"+file.getOriginalFilename();

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
                fileService.generateThumbnail(rutaArchivoOriginal, workdir + File.separator + "min"+ File.separator+"m_"+uuid+".jpg");
                fileService.sealImage(rutaArchivoOriginal,rutaArchivoFirmado,document);
            }else if(fileService.isVideo(fileext)){
                fileService.sealVideo(rutaArchivoOriginal,rutaArchivoFirmado,document);
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
        logger.info("Finish Sign:"+LocalDateTime.now());
        return responseDTO;
    }

    public ValidateResponseDTO validate(MultipartFile file){
        ValidateResponseDTO responseDTO=new ValidateResponseDTO();

        Path copyLocation = null;

        try {
            logger.info("Started validate:"+LocalDateTime.now());

            String uuid = UUID.randomUUID().toString();
            String fileName= uuid + "."+ FilenameUtils.getExtension(StringUtils.cleanPath(file.getOriginalFilename()));
            String rutaArchivoFirmado= workdir + File.separator + "tmp"+File.separator+"Sellado"+fileName;
            copyLocation = Paths.get(rutaArchivoFirmado);
            Files.copy(file.getInputStream(), copyLocation, StandardCopyOption.REPLACE_EXISTING);
            String fileext = fileService.changeFileExtension(FilenameUtils.getExtension(StringUtils.cleanPath(file.getOriginalFilename())).toLowerCase());

            String seal="";
            if(fileService.isStaticImage(fileext)) {
                seal = verifiyImageQr(rutaArchivoFirmado);
                return validateSealImage(seal,rutaArchivoFirmado,uuid,fileName);
            }
            else if(fileService.isVideo(fileext)){
                seal = verifiyVideoQr(rutaArchivoFirmado);
                return validateSeal(seal,rutaArchivoFirmado);
            }
            else{
                seal=fileService.getSeal(rutaArchivoFirmado);
                return validateSeal(seal,rutaArchivoFirmado);
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
                    logger.info("Error al eliminar archivo de trabajo");
                }
            }
        }
        logger.info("|validate|FinOK."+LocalDateTime.now());
        return responseDTO;
    }
    private ValidateResponseDTO validateSealImage(String seal,String rutaArchivoFirmado,String uuid,String fileName) throws Exception{
        ValidateResponseDTO responseDTO = new ValidateResponseDTO();
        if (!seal.equals("")) { // Exists QR
            Document document = gson.fromJson(seal, Document.class);
            Document documentBD = documentRepository.findById(document.getId()).orElse(null);
            if (documentBD != null) {
                logger.info("<4>Find with OpenCV . " + LocalDateTime.now());
                logger.info("Begin comparing...");
                String rutaArchivoQR = workdir + File.separator + "FilesSealed" + File.separator + documentBD.getUuid() + ".jpg";
                String rutaArchivoFakeSize = workdir + File.separator + "lib" + File.separator + "fake_size.jpg";
                String rutaArchivoDiffereFake = workdir + File.separator + "tmp" + File.separator + uuid + "_differeFake.jpg";
                double resCompare = fileService.compareContentImage(rutaArchivoQR, rutaArchivoFirmado, rutaArchivoFakeSize, rutaArchivoDiffereFake);
                double matchPorcentage = 100 - resCompare;
                logger.info("matchPorcentage: " + matchPorcentage);
                if (matchPorcentage > acceptancePercentage) {
                    User user = userRepository.getReferenceById(documentBD.getCreatedBy());
                    logger.info("File signed: " + rutaArchivoFirmado);

                    responseDTO.setStatus(0);
                    responseDTO.setCodeError("DOCU000");
                    responseDTO.setMsgError("OK");
                    responseDTO.setDocumentId(documentBD.getId());
                    responseDTO.setCreatedDate(documentBD.getCreatedDate());
                    responseDTO.setOriginalName(documentBD.getFileName());
                    responseDTO.setAuthor(user.getName());
                    responseDTO.setEmail(user.getEmail());
                    logger.info("|Val|ConQROK." + LocalDateTime.now());
                    return responseDTO;
                } else if (matchPorcentage > similarityPercentage) {
                    responseDTO.setStatus(1);
                    responseDTO.setCodeError("DOCU004");
                    responseDTO.setMsgError("Not an Alipsé Sealed File, yet it looks VERY MUCH LIKE\none of the images in our database by [author] (please be warned it’s not identical) "); // Firmado, pero  muy parecido.
                    responseDTO.setFileName(uuid + "_differeFake.jpg");

                    logger.info("|Val|SinQRcomparingFinFake." + LocalDateTime.now());
                    return responseDTO;
                } else {
                    responseDTO.setStatus(1);
                    responseDTO.setCodeError("DOCU003");
                    responseDTO.setMsgError("Not an Alipsé Sealed File, we cannot determine its authenticity"); // Firmado, pero no es el mismo.
                    responseDTO.setFileName(uuid + "_differeFake.jpg");

                    logger.info("|Val|SinQRComparingNotSaledFinFake." + LocalDateTime.now());
                    return responseDTO;
                }
            } else {
                responseDTO.setStatus(1);
                responseDTO.setCodeError("DOCU002");
                responseDTO.setMsgError("Alipsé Sealed FAKE file\n[the author] invests to avoid impersonation"); // Firma invalida
                responseDTO.setFileName("fake.jpg");

                logger.info("|Val|SinQRNotSaledFinFake." + LocalDateTime.now());
                return responseDTO;
            }
        }
        else {
            String rutaMiniatura = workdir + File.separator + "min" + File.separator + "mtmp_" + fileName;
            fileService.generateThumbnail(rutaArchivoFirmado, rutaMiniatura);
            CompareMinisResponseDTO compareMinisResponseDTO = compareMinis(rutaMiniatura);
            Files.delete(Paths.get(rutaMiniatura));
            double matchPorcentage = compareMinisResponseDTO.getMatchPercentage();
            String rutaArchivoDiffereFake = null;
            if (compareMinisResponseDTO.getMatchPercentage() >= acceptancePercentage) {
                String rutaArchivo = workdir + File.separator + "files" + File.separator + compareMinisResponseDTO.getDocument().getFileName();
                String rutaArchivoFakeSize = workdir + File.separator + "lib" + File.separator + "fake_size.jpg";
                rutaArchivoDiffereFake = workdir + File.separator + "tmp" + File.separator + uuid + "_differeFake.jpg";
                double resCompare = fileService.compareContentImage(rutaArchivo, rutaArchivoFirmado, rutaArchivoFakeSize, rutaArchivoDiffereFake);
                matchPorcentage = 100 - resCompare;
            }
            logger.info("matchPorcentage: " + matchPorcentage);
            if (matchPorcentage >= acceptancePercentage) {
                responseDTO.setDocumentId(compareMinisResponseDTO.getDocument().getId());
                responseDTO.setCreatedDate(compareMinisResponseDTO.getDocument().getCreatedDate());
                responseDTO.setOriginalName(compareMinisResponseDTO.getDocument().getFileName());
                responseDTO.setAuthor(compareMinisResponseDTO.getNameAuthor());
                responseDTO.setEmail(compareMinisResponseDTO.getEmailAuthor());

                responseDTO.setStatus(0);
                responseDTO.setCodeError("DOCU000");
                responseDTO.setMsgError("OK");

                logger.info("|Val|FinMiniOK." + LocalDateTime.now());
                return responseDTO;
            }
            else if (matchPorcentage > similarityPercentage) {
                responseDTO.setStatus(1);
                responseDTO.setCodeError("DOCU005");
                responseDTO.setMsgError("Not an Alipsé Sealed File, yet it looks VERY MUCH LIKE\n one of the images in our database by [author] (please be warned it’s not identical)"); //El documento contiene una firma no reconocida
                responseDTO.setFileName(uuid + "_differeFake.jpg");

                logger.info("|Val|FinMiniLike." + LocalDateTime.now());
                return responseDTO;
            }
            else {
                responseDTO.setStatus(1);
                responseDTO.setCodeError("DOCU001");
                responseDTO.setMsgError("Alipsé Sealed FAKE file\n [the author] invests to avoid impersonation"); //El documento contiene una firma no reconocida
                responseDTO.setFileName("fake.jpg");

                logger.info("|Val|FinMiniFake." + LocalDateTime.now());
                return responseDTO;
            }
        }
    }

    private ValidateResponseDTO validateSeal(String seal,String rutaArchivoFirmado){
        ValidateResponseDTO responseDTO = new ValidateResponseDTO();
        if(!seal.equals("")){
            logger.info("json: " + seal);
            //byte[] decodedBytes = Base64.decodeBase64(seal);
            //logger.info("decodedBytes " + new String(decodedBytes));
            //json=decodedBytes.toString();
            Document document = gson.fromJson(seal, Document.class);
            Document documentBD = documentRepository.findById(document.getId()).orElse(null);
            if (documentBD==null){
                responseDTO.setStatus(1);
                responseDTO.setCodeError("DOCU002");
                responseDTO.setMsgError("Alipsé Sealed FAKE file\n [the author] invests to avoid impersonation");
                responseDTO.setFileName("fake.jpg");

                logger.info("|Val|SinQRFake."+LocalDateTime.now());
                return responseDTO;
            }else if(!documentBD.getHashSignedDocument().equals(fileService.generateHash(rutaArchivoFirmado))) {
                responseDTO.setStatus(1);
                responseDTO.setCodeError("DOCU003");
                responseDTO.setMsgError("Not an Alipsé Sealed File, we cannot determine its authenticity");
                responseDTO.setFileName("fake.jpg");

                logger.info("|Val|SinQRFake."+LocalDateTime.now());
                return responseDTO;
            }else{
                User user = userRepository.getReferenceById(documentBD.getCreatedBy());
                logger.info("File signed: "+rutaArchivoFirmado);

                responseDTO.setStatus(0);
                responseDTO.setCodeError("DOCU000");
                responseDTO.setMsgError("OK");
                responseDTO.setDocumentId(documentBD.getId());
                responseDTO.setCreatedDate(documentBD.getCreatedDate());
                responseDTO.setOriginalName(documentBD.getFileName());
                responseDTO.setAuthor(user.getName());
                responseDTO.setEmail(user.getEmail());
                logger.info("|Val|SinQROK."+LocalDateTime.now());
                return responseDTO;
            }
        }else{
            String hash= fileService.generateHash(rutaArchivoFirmado);
            Document documentBD = documentRepository.findFirstByHashOriginalDocument(hash);
            if (documentBD==null){
                responseDTO.setStatus(1);
                responseDTO.setCodeError("DOCU002");
                responseDTO.setMsgError("Alipsé Sealed FAKE file\n [the author] invests to avoid impersonation");
                responseDTO.setFileName("fake.jpg");

                logger.info("|Val|SinQRFake."+LocalDateTime.now());
                return responseDTO;
            }else{
                User user = userRepository.getReferenceById(documentBD.getCreatedBy());
                logger.info("File signed: "+rutaArchivoFirmado);

                responseDTO.setStatus(0);
                responseDTO.setCodeError("DOCU000");
                responseDTO.setMsgError("OK");
                responseDTO.setDocumentId(documentBD.getId());
                responseDTO.setCreatedDate(documentBD.getCreatedDate());
                responseDTO.setOriginalName(documentBD.getFileName());
                responseDTO.setAuthor(user.getName());
                responseDTO.setEmail(user.getEmail());
                logger.info("|Val|SinQROK."+LocalDateTime.now());
                return responseDTO;
            }
        }
    }

    private CompareMinisResponseDTO compareMinis(String rutaArchivoMiniUpload) throws IOException {
        logger.info("Count repository: "+documentRepository.count());
        List<Document> documents = documentRepository.findAll();
        String rutaArchivoMini = "";
        Double minorDifference = 200.0,difference=200.0;
        Document documentBestMach = null;
        for(Document document : documents){
            rutaArchivoMini = workdir + File.separator + "min"+ File.separator+"m_"+document.getUuid()+".jpg";
            System.out.print("Validando existencia: "+rutaArchivoMini);
            File fileMiniInBD = new File(rutaArchivoMini);
            // Checking if the specified file exists or not
            if (fileMiniInBD.exists()) {
                logger.info(" Exists");
                difference = fileService.compareMinisProcess(rutaArchivoMiniUpload,rutaArchivoMini);
            }else{
                logger.info(" Does not Exists");
            }
            logger.info("difference: "+difference);
            logger.info("minorDifference: "+minorDifference);
            if(difference<minorDifference){
                minorDifference = difference;
                documentBestMach=document;
                logger.info("Menor diferencia ... : "+minorDifference);
            }
        }
        double bestMatch = 100 - minorDifference;
        logger.info("Mejor coincidencia: "+bestMatch);
        CompareMinisResponseDTO response = new CompareMinisResponseDTO();

        if(bestMatch> acceptancePercentage ){
            logger.info("Document of best match: " + documentBestMach);
            User author = userRepository.findById(documentBestMach.getCreatedBy()).orElse(null);;
            if(author!=null){
                response.setEmailAuthor(author.getEmail());
                response.setNameAuthor(author.getName());
            }
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
            ResponseEntity<VerifyImageQrResponseDTO> responseEntity = restTemplate.postForEntity(decodeqrDomain + "decodeqr/verifyImageQR", httpEntity,
                    VerifyImageQrResponseDTO.class);

            logger.info(responseEntity.getBody().getData());
            data=responseEntity.getBody().getData();

        }catch(Exception e){
            logger.info(e.getMessage());
        }
        return data;
    }

    public String verifiyVideoQr(String pathVideo){

        String pathImage=getFirstImageVideo(pathVideo);
        if(pathImage.isEmpty()){
            return "";
        }else{
            return verifiyImageQr(pathImage);
        }
    }

    public String getFirstImageVideo(String pathVideo){
        try {
            String pathImage = workdir + File.separator + "tmp"+File.separator+UUID.randomUUID().toString() + ".jpg";
            GetFirstImageVideoRequest request = new GetFirstImageVideoRequest();
            request.setInputVideoPath(pathVideo);
            request.setOutputImagePath(pathImage);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<GenericResponseDTO> responseEntity = restTemplate.postForEntity(decodeqrDomain + "decodeqr/getFirstImageVideo",
                    request, GenericResponseDTO.class);
            logger.info(responseEntity.getBody());
            GenericResponseDTO responseDTO=responseEntity.getBody();
            if(responseDTO== null || responseDTO.getStatus()!=0){
                throw new Exception("Error en decodeqr/getFirstImageVideo");
            }
            return pathImage;
        }catch(Exception e){
            logger.info(e.getMessage());
            return "";
        }
    }

    public void download(String fileName,HttpServletResponse response){
        try {
            File file = null;
            if(fileName.equals("fake.jpg")){
                file = new File(workdir + File.separator + "lib" + File.separator + "fake.jpg");
            }else if(fileName.contains("differeFake.jpg")){
                file = new File(workdir + File.separator + "tmp" + File.separator + fileName);
            }else{
                file = new File(workdir + File.separator + "FilesSealed" + File.separator + fileName);
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
                logger.info("filename: " + file.getName());
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
