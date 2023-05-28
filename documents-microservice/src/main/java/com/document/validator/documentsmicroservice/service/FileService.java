package com.document.validator.documentsmicroservice.service;

import com.document.validator.documentsmicroservice.entity.Document;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgproc.Imgproc.INTER_AREA;
import static org.opencv.imgproc.Imgproc.resize;

@Service
public class FileService {
    public static String uploadDir = "C:\\Temporal";
    public static String rutaqr = "C:\\Temporal\\QR";

    public String changeFileExtension(String filExtension){
        switch (filExtension){
            case "mp4":
                return "avi";
            case "wav":
                return "mp3";
            default:
                return filExtension;
        }
    }

    public boolean isStaticImage(String fileExtension){
        if(fileExtension.equals("jpg") || fileExtension.equals("jpeg") || fileExtension.equals("png") || fileExtension.equals("jfif")){
            return true;
        }else{
            return false;
        }


    }

    public void generateCompressImage(String inputImagePath,String outputImagePath) throws IOException {
        try {
            File inputImage = new File(inputImagePath);
            BufferedImage image = ImageIO.read(inputImage);
            OutputStream os = new FileOutputStream(new File(outputImagePath));
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            ImageWriter writer = (ImageWriter) writers.next();
            ImageOutputStream ios = ImageIO.createImageOutputStream(os);
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.99f);
            writer.write(null, new IIOImage(image, null, null), param);
            os.close();
            ios.close();
            writer.dispose();
            System.out.println("Se aplico compresión 0.99f");
        }catch (IOException e){
            System.out.println("Error en la compresión de imagenes");
            throw e;
        }
    }

    public void generateThumbnail(String inputImagePath,String outputImagePath){
        Mat resizeimage;
        Mat src  =  imread(inputImagePath);
        resizeimage = new Mat();
        Size scaleSize = new Size(200,100);
        resize(src, resizeimage, scaleSize , 0, 0, INTER_AREA);
        Imgcodecs.imwrite(outputImagePath , resizeimage);
        System.out.println("Generated little image");
    }

    public Integer getSizemaxImage(String inputImagePath){
        Mat src  =  imread(inputImagePath);
        return (src.cols()>src.rows())?src.cols() : src.rows();
    }

    public void sealImage(String inputImagePath, String outputImagePath, Document document) throws Exception{
        try {
            String signature = document.getId() 
                    + "|" + document.getHashOriginalDocument()
                    + "|" + document.getCreatedDate()
                    + "|" + document.getCreatedBy();
            
            String qrCodePath = rutaqr + File.separator + document.getUuid() + "_CodeQR.jpg";

            Integer sizemax = getSizemaxImage(inputImagePath);

            generateQR(signature, qrCodePath, sizemax);

            DocumentService.rutaArchivoOriginalQR=inputImagePath;
            File fileOrigen = new File(inputImagePath);
            BufferedImage imageOrigen = ImageIO.read(fileOrigen);
            System.out.println(qrCodePath);
            File fileQR = new File(qrCodePath);
            BufferedImage imageQR = ImageIO.read(fileQR);
            BufferedImage image3 = combineCodeAndBackImage(imageOrigen, imageQR, 15, 15);
        }catch (Exception e){
            System.out.println("Error en la generación del código QR");
            throw e;
        }
    }

    public static void generateQR(String data, String qrCodePath, Integer sizemax) throws IOException, WriterException {
        int sizeQR = sizemax/100*10; // 10% del tamaño máximo de la imagen
        sizeQR = (sizeQR<120)?120:sizeQR;
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put (EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // Establecer la tasa de tolerancia a fallas al valor predeterminado más alto
        hints.put (EncodeHintType.CHARACTER_SET, "UTF-8"); // La codificación de caracteres es UTF-8
        hints.put (EncodeHintType.MARGIN, 1); // El área en blanco del código QR, el mínimo es 0 y hay bordes blancos, pero es muy pequeño, el mínimo es alrededor del 6%

        BitMatrix matrix=new MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, sizeQR, sizeQR, hints);
        MatrixToImageWriter.writeToPath(matrix,"jpg", Paths.get(qrCodePath));

        ////
        Mat resizeimage;

        Mat src  =  imread(rutaqr + File.separator +"logo.jpg");
        resizeimage = new Mat();
        Size scaleSize = new Size(30,30);
        resize(src, resizeimage, scaleSize , 0, 0, INTER_AREA);
        Imgcodecs.imwrite(rutaqr + File.separator +"logo30x30.jpg", resizeimage);

        BufferedImage background1 = ImageIO.read(new File(qrCodePath));
        BufferedImage foreground1 = ImageIO.read(new File(rutaqr + File.separator + "logo30x30.jpg"));

        BufferedImage bufferedImage1 = new BufferedImage(background1.getWidth(),background1.getHeight(),BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d1 = bufferedImage1.createGraphics();

        g2d1.drawImage(background1, 0, 0,null);

        int x = (background1.getWidth() - foreground1.getWidth()) / 2;
        int y = (background1.getHeight() - foreground1.getHeight()) / 2;
        g2d1.drawImage(foreground1, x, y, null);
        g2d1.dispose();
        ImageIO.write(bufferedImage1,"JPEG", new File(qrCodePath));
    }

    private static BufferedImage combineCodeAndBackImage(BufferedImage codeImage, BufferedImage backImage, int marginLeft, int marginBottom) throws IOException {
        long start = System.currentTimeMillis();
        Graphics2D backImageGraphics = backImage.createGraphics();
        // Determine las coordenadas del código QR en la esquina superior izquierda de la imagen de fondo
        int x = marginLeft;
        if (marginLeft == -1) {
            x = (backImage.getWidth() - codeImage.getWidth()) / 2;
        }
        int y = backImage.getHeight() - codeImage.getHeight() - marginBottom;

        // Generate image with QR code
        FileService.TestPane test=  (new FileService.TestPane());

        return backImage;
    }

    public static class TestPane extends JPanel {

        private BufferedImage background;
        private BufferedImage foreground;

        private BufferedImage finishimage;
        //File outputfile = new File("C:\\Gerardo\\Workspace\\Contenido\\ImgSealed\\"+uuid+"QR.jpg");
        File outputfile = new File(uploadDir+ File.separator +"ImgSealed"+ File.separator +DocumentService.uuid+"QR.jpg");

        public TestPane() throws IOException {
            try {
                System.out.println("Open images to convinated");
                background = ImageIO.read(new File(DocumentService.rutaArchivoOriginalQR));
                System.out.println("Ruta archivo para QR :"+DocumentService.rutaArchivoOriginalQR);
                System.out.println("Foreground:"+uploadDir+ File.separator+"QR"+File.separator+DocumentService.uuid+"_CodeQR.jpg");
                foreground = ImageIO.read(new File(uploadDir+ File.separator+"QR"+File.separator+DocumentService.uuid+"_CodeQR.jpg"));
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            // Dimension redim = new Dimension(background.getWidth(), background.getHeight());

            System.out.println("Convining images.. ");
            BufferedImage bufferedImage = new BufferedImage(background.getWidth(),background.getHeight(),BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = bufferedImage.createGraphics();

            //  if (background != null) {
            //int x1= (getWidth() - background.getWidth()) / 2;
            //int y1 = (getHeight() - background.getHeight()) / 2;
            int x1 = 0, y1 = 0;
            g2d.drawImage(background, x1, y1, this);
            // }
            // if (foreground != null) {
            int x = (getWidth() - foreground.getWidth()) / 2;
            int y = (getHeight() - foreground.getHeight()) / 2;
            //x = (background.getWidth() / 2) - (foreground.getWidth() / 2);
            //y = (background.getHeight() / 2) - (foreground.getHeight() / 2);
            x = 15; ////(background.getWidth() / 2) - (foreground.getWidth() / 2);
            y = (background.getHeight() -15) - (foreground.getHeight() );

            g2d.drawImage(foreground, x, y, this);
            // }
            g2d.dispose();
            try {
                ImageIO.write(bufferedImage,"JPEG",  outputfile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Path fileQRLocation = Paths.get(uploadDir+ File.separator+"QR"+File.separator+DocumentService.uuid+"_CodeQR.jpg" );
            Files.delete(fileQRLocation); // eliminate temp differ
            System.out.println("Elimina el archivo QR 11: ");
        }
    }
}
