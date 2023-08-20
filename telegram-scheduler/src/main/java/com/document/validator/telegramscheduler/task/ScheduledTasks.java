package com.document.validator.telegramscheduler.task;

import com.document.validator.telegramscheduler.dto.ValidateResponseDTO;
import com.google.gson.Gson;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Component
public class ScheduledTasks {

    @Value("${app.documents-microservice.domain}")
    private String documentsDomain;

    @Value("${app.telegramInPath}")
    private String telegramInPath;

    @Value("${app.telegramOutPath}")
    private String telegramOutPath;

    Logger logger = LogManager.getLogger(getClass());

    Gson gson = new Gson();

    public void checkInputFolder() throws IOException, InterruptedException{
        logger.info("documentsDomain: " + documentsDomain);
        logger.info("telegramInPath: " + telegramInPath);
        logger.info("telegramOutPath: " + telegramOutPath);

        File carpeta = new File(telegramInPath);
        File[] lista = carpeta.listFiles();

        if(lista.length > 0) {
            logger.info("Number of files in telegramInPath: " + lista.length);
            
            for (File file : lista) {
                logger.info("Start to Procces file " + file.getName());

                ValidateResponseDTO validateResponseDTO = validateFile(file.getPath());

                if(validateResponseDTO!=null){
                    String outputFileName="";
                    String extesion="";
                    Path inputLocation = null;
                    Path outputLocation = null;
                    InputStream inputStream = null;
                    switch (validateResponseDTO.getCodeError()){
                        case "DOCU000":
                            outputFileName = file.getName();
                            outputLocation = Paths.get(telegramOutPath + File.separator + outputFileName);
                            inputLocation = Paths.get(file.getPath());
                            Files.copy(inputLocation, outputLocation, StandardCopyOption.REPLACE_EXISTING);
                            break;
                        case "DOCU001":
                        case "DOCU002":
                        case "DOCU003":
                            extesion=FilenameUtils.getExtension(StringUtils.cleanPath(validateResponseDTO.getFileName()));
                            outputFileName = file.getName().replace("." + extesion,"") + "_notsealed.jpg";
                            outputLocation = Paths.get(telegramOutPath + File.separator + outputFileName);
                            inputStream=downloadFile(validateResponseDTO.getFileName());
                            Files.copy(inputStream, outputLocation, StandardCopyOption.REPLACE_EXISTING);
                            break;
                        case "DOCU004":
                        case "DOCU005":
                            extesion=FilenameUtils.getExtension(StringUtils.cleanPath(validateResponseDTO.getFileName()));
                            outputFileName = file.getName().replace("." + extesion,"") + "_differeFake.jpg";
                            outputLocation = Paths.get(telegramOutPath + File.separator + outputFileName);
                            inputStream=downloadFile(validateResponseDTO.getFileName());
                            Files.copy(inputStream, outputLocation, StandardCopyOption.REPLACE_EXISTING);
                            break;
                    }
                    logger.info("outputFileName: " + outputFileName);
                }

                file.delete();

                logger.info("Finish to proccess file " + file.getName());
            }
        }
    }

    public ValidateResponseDTO validateFile(String pathfile){
        ValidateResponseDTO validateResponseDTO=null;
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
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(documentsDomain + "document/validate", httpEntity,
                    String.class);

            logger.info(responseEntity.getBody());
            validateResponseDTO = gson.fromJson(responseEntity.getBody(), ValidateResponseDTO.class);

        }catch(Exception e){
            logger.info(e.getMessage());
        }
        return validateResponseDTO;
    }

    public InputStream downloadFile(String fileName){
        InputStream file=null;
        try {
            Client client = ClientBuilder.newClient();
            String url = documentsDomain + "document/download/" + fileName;
            file = client.target(url).request().get(InputStream.class);

        }catch(Exception e){
            logger.info(e.getMessage());
        }
        return file;
    }
}