package com.document.validator.decodeqrmicroservice.service;

import com.document.validator.decodeqrmicroservice.dto.VerifyImageQrResponseDTO;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.QRCodeDetector;
import org.springframework.stereotype.Service;
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
    public static String uploadDir = "C:\\Temporal";

    public VerifyImageQrResponseDTO verifyImageQR(MultipartFile file) {
        System.out.println("OpenCV Version: " + Core.VERSION);
        System.out.println("|Sign|Ini."+ LocalDateTime.now());

        String uuid = UUID.randomUUID().toString();
        String rutaArchivoOriginal=uploadDir + File.separator + "Img" + File.separator + file.getOriginalFilename();

        VerifyImageQrResponseDTO responseDTO =new VerifyImageQrResponseDTO();
        String res = "";
        try {
            Path copyLocationOri = Paths.get(rutaArchivoOriginal);
            Files.copy(file.getInputStream(), copyLocationOri, StandardCopyOption.REPLACE_EXISTING);

            // String pathfile = "C:\\Gerardo\\Workspace\\Contenido\\ImgSealed\\d28ade1d-c73b-49a4-b116-090cd55bff4fQR.jpg"; // Con QR
            //pathfile = "C:\\Gerardo\\Workspace\\Contenido\\ImgSealed\\d28ade1d-c73b-49a4-b116-090cd55bff4fQR.jpg"; // Sin QR
            System.out.println(rutaArchivoOriginal);
            Mat img = Imgcodecs.imread(rutaArchivoOriginal);
            QRCodeDetector decoder = new QRCodeDetector();
            Mat points = new Mat();
            decoder.detect(img, points);

            String data = decoder.detectAndDecode(img, points);
            System.out.println("Intento (0) resultado...: "+ points.empty());
            if (!points.empty() && data.length()>0) {
                System.out.println("QR detected... " );
                System.out.println("Data..:" +data);

                res = data;
            } else {
                //Intentando redimensionar para encontrar el QR
                Mat dst = new Mat();
                Double fac = Double.valueOf(0);
                System.out.println("Entra a redim para buscar QR..");
                for(double i=1; i<12; i++) {
                    fac = (Double)(1+(i/10));
                    System.out.print(i);
                    Imgproc.resize(img, dst, new Size(0, 0), fac, fac, Imgproc.INTER_AREA);
                    data = decoder.detectAndDecode(dst, points);
                    if(!points.empty()&&data.length()>0){ //QR detected
                        System.out.println("Data..:" +data);
                        res=data;
                        break;
                    }
                }
                System.out.println("QR detected... "+!points.empty());
                if (points.empty()||data.length()<1){
                    for(double i=1; i<11; i++) {
                        fac = (Double)(1+(i/2));
                        System.out.print("Resize up:" +i);
                        Imgproc.resize(img, dst, new Size(0, 0), fac, fac, Imgproc.INTER_AREA);
                        data = decoder.detectAndDecode(dst, points);
                        if(!points.empty()&&data.length()>0){ //QR detected
                            System.out.println("Data..:" +data);
                            res=data;
                            break;
                        }
                    }
                }
                System.out.println("QR detected... "+!points.empty());
            }
        } catch (Exception e) {
            System.out.println("Error in trying to open file:" + e.toString());
        }
        responseDTO.setData(res);
        return responseDTO;
    }
}
