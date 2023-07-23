package com.document.validator.decodeqrmicroservice.service;

import com.document.validator.decodeqrmicroservice.dto.GenericResponseDTO;
import com.document.validator.decodeqrmicroservice.dto.GetFirstImageVideoRequest;
import com.document.validator.decodeqrmicroservice.dto.SingVideoRequest;
import com.document.validator.decodeqrmicroservice.dto.VerifyImageQrResponseDTO;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.Date;

@Service
public class DecodeQrService {
    @Value("${app.workdir}")
    public String workdir;

    @Value("${app.ffmpegPath}")
    public String ffmpegPath;

    @Value("${app.ffprobePath}")
    public String ffprobePath;

    Logger logger = LogManager.getLogger(getClass());

    public VerifyImageQrResponseDTO verifyImageQr(MultipartFile file){
        VerifyImageQrResponseDTO responseDTO =new VerifyImageQrResponseDTO();
        try {
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(
                    new BufferedImageLuminanceSource(
                            ImageIO.read(file.getInputStream()))));
            Result qrCodeResult = new MultiFormatReader().decode(binaryBitmap);
            System.out.println("Codigo QR: " + qrCodeResult.getText());
            responseDTO.setData(qrCodeResult.getText());
            responseDTO.setStatus(0);
            responseDTO.setCodeError("DECODEQR000");
            responseDTO.setMsgError("OK");
        } catch (IOException | NotFoundException ex) {
            ex.printStackTrace();
            responseDTO.setData("");
            responseDTO.setStatus(1);
            responseDTO.setCodeError("DECODEQR001");
            responseDTO.setMsgError(ex.getMessage());
        }
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
