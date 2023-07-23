package com.document.validator.decodeqrmicroservice.service;

import com.document.validator.decodeqrmicroservice.dto.GenericResponseDTO;
import com.document.validator.decodeqrmicroservice.dto.GetFirstImageVideoRequest;
import com.document.validator.decodeqrmicroservice.dto.SingVideoRequest;
import com.document.validator.decodeqrmicroservice.dto.VerifyImageQrResponseDTO;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.QRCodeDetector;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Service
public class DecodeQrService {
    @Value("${app.workdir}")
    public String workdir;

    @Value("${app.ffmpegPath}")
    public String ffmpegPath;

    @Value("${app.ffprobePath}")
    public String ffprobePath;

    Logger logger = LogManager.getLogger(getClass());

    public VerifyImageQrResponseDTO verifyImageQR(MultipartFile file) {
        logger.info("OpenCV Version: " + Core.VERSION);
        logger.info("|Sign|Ini."+ LocalDateTime.now());

        String uuid = UUID.randomUUID().toString();
        String rutaArchivoOriginal= workdir + File.separator + "tmp" + File.separator +
                uuid + "."+ FilenameUtils.getExtension(StringUtils.cleanPath(file.getOriginalFilename()));

        VerifyImageQrResponseDTO responseDTO =new VerifyImageQrResponseDTO();
        String res = "";
        try {
            Path copyLocationOri = Paths.get(rutaArchivoOriginal);
            Files.copy(file.getInputStream(), copyLocationOri, StandardCopyOption.REPLACE_EXISTING);

            logger.info(rutaArchivoOriginal);
            Mat img = Imgcodecs.imread(rutaArchivoOriginal);
            logger.info("img generado");
            QRCodeDetector decoder = new QRCodeDetector();
            logger.info("decoder generado");
            Mat points = new Mat();
            logger.info("points generado");
            decoder.detect(img, points);
            logger.info("decoder.detect generado");
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

    public GenericResponseDTO addQRVideo(SingVideoRequest request){
        GenericResponseDTO responseDTO = new GenericResponseDTO();
        try {
            logger.info("request:" + request);

            FFmpeg ffmpeg = new FFmpeg(ffmpegPath);
            FFprobe ffprobe = new FFprobe(ffprobePath);
            FFmpegBuilder builder = new FFmpegBuilder();
            builder.addInput(request.getInputVideoPath());
            builder.addInput(request.getQrCodePath());
            builder.setComplexFilter("[0:v][1:v]overlay=eof_action=pass:enable='between(t,0,2)'");
            builder.addOutput(request.getOutputVideoPath());
            builder.overrideOutputFiles(true);
            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
            logger.info(new Date());
            // Run a one-pass encode
            executor.createJob(builder).run();
            logger.info(new Date());


            logger.info("Se sello el video en: "+request.getOutputVideoPath());

            responseDTO.setStatus(0);
            responseDTO.setCodeError("VIDEO000");
            responseDTO.setMsgError("OK");

        }catch (Exception e){
            logger.info("Error:" + e.getMessage());

            responseDTO.setStatus(1);
            responseDTO.setCodeError("VIDEO003");
            responseDTO.setMsgError("Error en el sellado deL video");
        }
        return responseDTO;
    }

    public GenericResponseDTO getFirstImageVideo(GetFirstImageVideoRequest request){
        GenericResponseDTO responseDTO = new GenericResponseDTO();
        try {
            System.out.println("request:" + request);

            VideoCapture videoCapture = new VideoCapture(request.getInputVideoPath());
            if(!videoCapture.isOpened()){
                responseDTO.setStatus(1);
                responseDTO.setCodeError("VIDEO001");
                responseDTO.setMsgError("No se pudo abrir el video");
            }

            Mat firstFrame = new Mat();
            if(!videoCapture.read(firstFrame)){
                responseDTO.setStatus(1);
                responseDTO.setCodeError("VIDEO002");
                responseDTO.setMsgError("No se pudo obtener el 1er frame");
            }

            Imgcodecs.imwrite(request.getOutputImagePath() , firstFrame);

            responseDTO.setStatus(0);
            responseDTO.setCodeError("VIDEO000");
            responseDTO.setMsgError("OK");

        }catch (Exception e){
            responseDTO.setStatus(1);
            responseDTO.setCodeError("VIDEO003");
            responseDTO.setMsgError("Error en el sellado deL video");
        }
        return responseDTO;
    }
}
