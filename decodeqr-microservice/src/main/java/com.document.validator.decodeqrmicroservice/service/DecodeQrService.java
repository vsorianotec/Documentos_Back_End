package com.document.validator.decodeqrmicroservice.service;

import com.document.validator.decodeqrmicroservice.dto.VerifyImageQrResponseDTO;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.QRCodeDetector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DecodeQrService {
    @Value("${app.workdir}")
    public String workdir;

    Logger logger = LogManager.getLogger(getClass());

    public VerifyImageQrResponseDTO verifyImageQR(MultipartFile file) {
        logger.info("OpenCV Version: " + Core.VERSION);
        logger.info("|Sign|Ini."+ LocalDateTime.now());

        String uuid = UUID.randomUUID().toString();
        String rutaArchivoOriginal= workdir + File.separator + "tmp" + File.separator +
                uuid + FilenameUtils.getExtension(StringUtils.cleanPath(file.getOriginalFilename()));

        VerifyImageQrResponseDTO responseDTO =new VerifyImageQrResponseDTO();
        String res = "";
        try {
            Path copyLocationOri = Paths.get(rutaArchivoOriginal);
            Files.copy(file.getInputStream(), copyLocationOri, StandardCopyOption.REPLACE_EXISTING);

            logger.info(rutaArchivoOriginal);
            Mat img = Imgcodecs.imread(rutaArchivoOriginal);
            QRCodeDetector decoder = new QRCodeDetector();
            Mat points = new Mat();
            decoder.detect(img, points);

            String data = decoder.detectAndDecode(img, points);
            logger.info("Intento (0) resultado...: "+ points.empty());
            if (!points.empty() && data.length()>0) {
                logger.info("QR detected... " );
                logger.info("Data..:" +data);

                res = data;
            } else {
                //Intentando redimensionar para encontrar el QR
                Mat dst = new Mat();
                Double fac = Double.valueOf(0);
                logger.info("Entra a redim para buscar QR..");
                for(double i=1; i<12; i++) {
                    fac = (Double)(1+(i/10));
                    System.out.print(i);
                    Imgproc.resize(img, dst, new Size(0, 0), fac, fac, Imgproc.INTER_AREA);
                    data = decoder.detectAndDecode(dst, points);
                    if(!points.empty()&&data.length()>0){ //QR detected
                        logger.info("Data..:" +data);
                        res=data;
                        break;
                    }
                }
                logger.info("QR detected... "+!points.empty());
                if (points.empty()||data.length()<1){
                    for(double i=1; i<11; i++) {
                        fac = (Double)(1+(i/2));
                        System.out.print("Resize up:" +i);
                        Imgproc.resize(img, dst, new Size(0, 0), fac, fac, Imgproc.INTER_AREA);
                        data = decoder.detectAndDecode(dst, points);
                        if(!points.empty()&&data.length()>0){ //QR detected
                            logger.info("Data..:" +data);
                            res=data;
                            break;
                        }
                    }
                }
                logger.info("QR detected... "+!points.empty());
            }
        } catch (Exception e) {
            logger.info("Error in trying to open file:" + e.toString());
        }
        responseDTO.setData(res);
        return responseDTO;
    }
}
