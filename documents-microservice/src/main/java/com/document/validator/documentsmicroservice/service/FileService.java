package com.document.validator.documentsmicroservice.service;

import com.document.validator.documentsmicroservice.entity.Document;
import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.List;

import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgproc.Imgproc.INTER_AREA;
import static org.opencv.imgproc.Imgproc.resize;

@Service
public class FileService {

    @Value("${app.workdir}")
    public String workdir;

    @Value("${app.ffmpegPath}")
    public String ffmpegPath;

    @Value("${app.ffprobePath}")
    public String ffprobePath;

    Logger logger = LogManager.getLogger(getClass());

    Gson gson = new Gson();

    public String changeFileExtension(String filExtension){
        switch (filExtension){
            //case "mp4":
            //    return "avi";
            case "wav":
                return "mp3";
            case "jpeg":
            case "png":
            case "jfif":
                return "jpg";
            default:
                return filExtension;
        }
    }

    public boolean isStaticImage(String fileExtension){
        fileExtension = fileExtension.toLowerCase(Locale.ROOT);
        return fileExtension.equals("jpg") || fileExtension.equals("jpeg") || fileExtension.equals("png") || fileExtension.equals("jfif");
    }
    public boolean isVideo(String fileExtension){
        fileExtension = fileExtension.toLowerCase(Locale.ROOT);
        return fileExtension.equals("mp4") || fileExtension.equals("avi");
    }
    public boolean isPDF(String fileExtension){
        fileExtension = fileExtension.toLowerCase(Locale.ROOT);
        return fileExtension.equals("pdf");

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
            logger.info("Se aplico compresión 0.99f");
        }catch (IOException e){
            logger.info("Error en la compresión de imagenes");
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
        logger.info("Generated little image");
    }

    public Integer getSizemaxImage(String inputImagePath){
        Mat src  =  imread(inputImagePath);
        return (src.cols()>src.rows())?src.cols() : src.rows();
    }

    public void sealImage(String inputImagePath, String outputImagePath, Document document) throws Exception{
        try {
            //String signature = gson.toJson(document);
            String signature = document.getUuid();
            String qrCodePath = workdir + File.separator + "tmp" + File.separator + document.getUuid() + "_CodeQR.jpg";
            Integer sizemax = getSizemaxImage(inputImagePath);
            generateQR(signature, qrCodePath, sizemax);
            combineImageAndQR(inputImagePath,qrCodePath,outputImagePath);

            Path fileQRLocation = Paths.get(qrCodePath);
            Files.delete(fileQRLocation);
            logger.info("Imagen Sellada ");
        }catch (Exception e){
            logger.info("Error en el sellado de la Imagen");
            throw e;
        }
    }

    public void sealPDF(String inputImagePath, String outputImagePath, Document document) throws Exception{
        try {
            //String signature = gson.toJson(document);
            String signature = document.getUuid();
            String qrCodePath = workdir + File.separator + "tmp" + File.separator + document.getUuid() + "_CodeQR.jpg";
            generateQR(signature, qrCodePath, 0);
            combinePDFAndQR(inputImagePath,qrCodePath,outputImagePath);

            Path fileQRLocation = Paths.get(qrCodePath);
            Files.delete(fileQRLocation);
            logger.info("Imagen Sellada ");
        }catch (Exception e){
            logger.info("Error en el sellado de la Imagen");
            throw e;
        }
    }

    public void sealFile(String inputImagePath, String outputImagePath, Document document) throws Exception{

        //String selloAlipse = "--AliPse" + Base64.encodeBase64(gson.toJson(document)) + "EOS--";
        String selloAlipse = "--AliPse" + gson.toJson(document) + "EOS--";
        logger.info("selloAlipse: " + selloAlipse);

        // Se abre el fichero original para lectura
        FileInputStream fileInput = new FileInputStream(inputImagePath);
        BufferedInputStream bufferedInput = new BufferedInputStream(fileInput);


        // Se abre el fichero donde se hará la copia
        FileOutputStream fileOutput = new FileOutputStream(outputImagePath);
        BufferedOutputStream bufferedOutput = new BufferedOutputStream(fileOutput);

        // Bucle para leer de un fichero y escribir en el otro.
        int paquetes = (int) (Files.size(Paths.get(inputImagePath)) / 256);
        byte[] array = new byte[256];
        int leidos = bufferedInput.read(array);
        int veces = 1;
        while (leidos > 0) {
            bufferedOutput.write(array, 0, leidos);
            leidos = bufferedInput.read(array);
            if (veces > paquetes - 1) {
                // Insertar sello AliPsé
                bufferedOutput.write(selloAlipse.getBytes());
                // Inserta último bloque de bytes
                bufferedOutput.write(array, 0, leidos);
                leidos = bufferedInput.read(array);
                break;
            }
            veces++;
        }

        // Cierre de los ficheros
        bufferedInput.close();
        bufferedOutput.close();
    }

    public void generateQR(String data, String qrCodePath, Integer sizemax) throws IOException, WriterException {
        try {
            int sizeQR = sizemax/100*10; // 10% del tamaño máximo de la imagen
            sizeQR = (sizeQR<120)?120:sizeQR;

            createQR(data,qrCodePath,"UTF-8",sizeQR,sizeQR);
        }catch (Exception e){
            logger.info("Error en la generación del código QR");
            throw e;
        }
    }

    // Function to create the QR code
    public static void createQR(String data, String path,
                                String charset,
                                int height, int width)
            throws WriterException, IOException
    {

        BitMatrix matrix = new MultiFormatWriter().encode(
                new String(data.getBytes(charset), charset),
                BarcodeFormat.QR_CODE, width, height);

        MatrixToImageWriter.writeToFile(
                matrix,
                path.substring(path.lastIndexOf('.') + 1),
                new File(path));
    }

    public void combineImageAndQR(String inputImagePath, String qrCodePath, String outputImagePath) throws IOException{
        try {

            File outputfile = new File(outputImagePath);

            logger.info("Open images to convinated");
            BufferedImage background = ImageIO.read(new File(inputImagePath));
            logger.info("Ruta archivo para QR :"+inputImagePath);
            logger.info("Foreground: "+ qrCodePath);
            BufferedImage foreground = ImageIO.read(new File(qrCodePath));

            logger.info("Convining images.. ");
            BufferedImage bufferedImage = new BufferedImage(background.getWidth(),background.getHeight(),BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = bufferedImage.createGraphics();

            JPanel panel = new JPanel();

            int x1 = 0, y1 = 0;
            g2d.drawImage(background, x1, y1, panel);

            int x = 0;
            //int y = 0;
            int y = background.getHeight() - foreground.getHeight();
            g2d.drawImage(foreground, x, y, panel);

            g2d.dispose();
            ImageIO.write(bufferedImage,"JPEG",  outputfile);

        } catch (IOException e) {
            logger.info("Error en la combinación de la Imagen y el QR");
            throw e;
        }
    }

    public String generateHash(String rutaArchivo) {
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

    public String getSeal(String rutaArchivoFirmado){
        FileReader archivo = null;
        BufferedReader br= null;
        try {
            // Apertura del fichero y creación de BufferedReader para poder
            // hacer una lectura comoda (disponer del método readLine()).
            archivo = new FileReader(rutaArchivoFirmado);
            br= new BufferedReader(archivo);

            // Lectura del fichero
            String linea;
            String lastLine="";

            int firma = -1, fin = -1;

            while((linea=br.readLine())!=null) {
                //logger.info(linea);
                lastLine=linea;
                firma = lastLine.indexOf("--AliPse");
                fin = lastLine.indexOf("EOS--");
                if(firma>-1){
                    System.out.print(lastLine.substring(firma + 8));
                    if(fin<0){
                        lastLine=lastLine+br.readLine();
                    }
                    break;
                }
            }
            archivo.close();
            firma = lastLine.indexOf("--AliPse");
            fin = lastLine.indexOf("EOS--");
            logger.info("Validando firma AliPse");
            logger.info("Firma: " + firma + " - " + fin);
            return lastLine.substring(firma + 8, fin);
        }catch(Exception e){
            return "";
        }finally{
            // En el finally cerramos el fichero, para asegurarnos
            // que se cierra tanto si todo va bien como si salta
            // una excepcion.
            try{
                if( br != null)
                    br.close();
                if( archivo != null )
                    archivo.close();
            }catch (Exception e2){
                e2.printStackTrace();
            }
        }
    }

    public double compareMinisProcess(String rutaArchivoMiniUpload, String rutaArchivoMini) throws IOException {
        Mat imgs = new Mat();
        Mat erodeImg = new Mat();
        Mat dilateImg = new Mat();
        Mat threshImg = new Mat();
        java.util.List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        Mat hierarchy = new Mat();

        Imgcodecs Highgui1 = null;
        logger.info("Validar:"+rutaArchivoMiniUpload);
        logger.info("Ruta archivos minis: " +rutaArchivoMini);


        Mat img1 = Highgui1.imread(rutaArchivoMiniUpload);
        Mat img2 = Highgui1.imread(rutaArchivoMini);

        Core.absdiff(img1, img2, imgs);

        Mat kernel = Imgproc.getStructuringElement(1,new Size(4,6));
        Mat kernel1 = Imgproc.getStructuringElement(1,new Size(2,3));
        // corrosión
        Imgproc.erode(imgs, erodeImg, kernel);
        // Expansión
        Imgproc.dilate(erodeImg, dilateImg, kernel1);
        // detectar borde
        Imgproc.threshold(dilateImg, threshImg, 20, 255, Imgproc.THRESH_BINARY);
        // Convertir a escala de grises
        Imgproc.cvtColor(threshImg, threshImg, Imgproc.COLOR_RGB2GRAY);
        // Encuentra el esquema (3: CV_RETR_TREE, 2: CV_CHAIN_APPROX_SIMPLE)
        Imgproc.findContours(threshImg, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        logger.info("Contornos diferentes: "+contours.size());

        List<Rect> boundRect = new ArrayList<Rect>(contours.size());
        String[] size = null;
        double  areaModif = 0;
        for(int i=0;i<contours.size();i++){
            // Genera un rectángulo envolvente externo basado en el contorno
            Rect rect = Imgproc.boundingRect(contours.get(i));
            boundRect.add(rect);
            logger.info("Rectangle : " + rect+" Size: "+rect.size().toString());
            size=rect.size().toString().split("x");
            logger.info("Lado:"+size[0]+" Alto:"+size[1]);
            areaModif=areaModif+(Double.parseDouble(size[0]) * Double.parseDouble(size[1]));
        }
        logger.info("Area modificada: "+areaModif);
        logger.info("Area de la imagen: "+img1.rows()*img1.cols());
        logger.info("% "+ (areaModif/(img1.rows()*img1.cols()))*100);
        for(int i=0;i<contours.size();i++){
            Scalar color = new Scalar(0,0,255);
            // Dibujar contorno
            //Imgproc.drawContours(img222, contours, i, color, 1, Core.LINE_8, hierarchy, 0, new Point());
            // Dibujar rectángulo
            Imgproc.rectangle(img2, boundRect.get(i).tl(), boundRect.get(i).br(), color, 2, Imgproc.LINE_8, 0);

        }

        // Reglas de negocio
        double differ = (areaModif/(img1.rows()*img1.cols()))*100;
        logger.info(differ);

        // Se borra la imagen con la diferencia
        //Path filedifferLocation = Paths.get(uploadDir + File.separator+"tmp"+File.separator + uuid + "_differe.jpg" );
        //Files.delete(filedifferLocation); // eliminate temp differ
        //logger.info("Elimina el archivo Differ 10: ");


        return differ;
    }

    public double compareContentImage(String rutaArchivo, String rutaArchivoFirmado,
                                      String rutaArchivoFakeSize, String rutaArchivoDiffereFake) throws IOException {


        logger.info("rutaArchivo -" + rutaArchivo);
        logger.info(rutaArchivoFirmado + "-" + rutaArchivoFirmado);

        //Metodo escala de grises

        Imgcodecs Highgui = null;
        Mat img111 = Highgui.imread(rutaArchivo);
        Mat img222 = Highgui.imread(rutaArchivoFirmado);

        if(img111.rows()!= img222.rows() || img111.cols()!=img222.cols()){ // Las imagenes tienen tamaños diferentes se aplica resize
            //Mat resizeimage = new Mat();
            Size scaleSize = new Size(img111.cols(),img111.rows());
            resize(img222, img222, scaleSize , 0, 0, INTER_AREA);
            logger.info("Redim " +img222.size());
        }

        Mat img = new Mat();
        // Los píxeles son pobres
        Core.absdiff(img111, img222, img);
        //Highgui.imwrite(rutaArchivoDiff, img);

        Mat imgs = new Mat();
        Mat erodeImg = new Mat();
        Mat dilateImg = new Mat();
        Mat threshImg = new Mat();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        Mat hierarchy = new Mat();
        // Los píxeles son pobres
        Core.absdiff(img111, img222, imgs);

        Mat kernel = Imgproc.getStructuringElement(1,new Size(4,6));
        Mat kernel1 = Imgproc.getStructuringElement(1,new Size(2,3));
        // corrosión
        Imgproc.erode(img, erodeImg, kernel);
        // Expansión
        Imgproc.dilate(erodeImg, dilateImg, kernel1);
        // detectar borde
        Imgproc.threshold(dilateImg, threshImg, 20, 255, Imgproc.THRESH_BINARY);
        // Convertir a escala de grises
        Imgproc.cvtColor(threshImg, threshImg, Imgproc.COLOR_RGB2GRAY);
        // Encuentra el esquema (3: CV_RETR_TREE, 2: CV_CHAIN_APPROX_SIMPLE)
        Imgproc.findContours(threshImg, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        logger.info("Contornos diferentes: "+contours.size());
        List<Rect> boundRect = new ArrayList<Rect>(contours.size());
        String[] size = null;
        double  areaModif = 0;
        for(int i=0;i<contours.size();i++){
            // Genera un rectángulo envolvente externo basado en el contorno
            Rect rect = Imgproc.boundingRect(contours.get(i));
            boundRect.add(rect);
            logger.info("Rectangle : " + rect+" Size: "+rect.size().toString());
            size=rect.size().toString().split("x");
            logger.info("Lado:"+size[0]+" Alto:"+size[1]);
            areaModif=areaModif+(Double.parseDouble(size[0]) * Double.parseDouble(size[1]));
        }
        logger.info("Area modificada: "+areaModif);
        logger.info("Area de la imagen: "+img111.rows()*img111.cols());
        logger.info("% "+ (areaModif/(img111.rows()*img111.cols()))*100);
        for(int i=0;i<contours.size();i++){
            Scalar color = new Scalar(0,0,255);
            // Dibujar rectángulo
            Imgproc.rectangle(img222, boundRect.get(i).tl(), boundRect.get(i).br(), color, 2, Imgproc.LINE_8, 0);

        }
        // Highgui.imwrite(rutaArchivoDiffere, img222);

        // Reglas de negocio
        double differ = (areaModif/(img111.rows()*img111.cols()))*100;
        logger.info(differ);

        double alfa=0.7,beta;
        Mat src1,src2,dst;

        //src1 = Highgui.imread(rutaArchivoDiffere);
        src1 = img222;
        src2 = Highgui.imread(rutaArchivoFakeSize);

        if( src1.empty() ) { logger.info("Error Cargando imagen1 n");}
        if( src2.empty() ) { logger.info("Error Cargando imagen2 n");}

        if(src1.rows()!= src2.rows() || src1.cols()!=src2.cols()){ // Las imagenes tienen tamaños diferentes se aplica resize
            Size scaleSize = new Size(src1.cols(),src1.rows());
            resize(src2, src2, scaleSize , 0, 0, INTER_AREA);
            logger.info("Redim " +src2.size());
        }
        dst = new Mat();
        beta = 1.0 - alfa;
        Core.addWeighted(src1, alfa, src2, beta, 0.0, dst);
        Highgui.imwrite(rutaArchivoDiffereFake , dst);

        return differ;
    }

    public void sealVideo(String inputVideoPath, String outputVideoPath, Document document) throws Exception{
        try {
            String qrCodePath = workdir + File.separator + "tmp" + File.separator + document.getUuid() + "_CodeQR.jpg";
            String firstFramePath = workdir + File.separator + "tmp" + File.separator + document.getUuid() + "_firstFrame.jpg";

            //Generamos QR
            //String signature = gson.toJson(document);
            String signature = document.getUuid();
            getFirstImageVideo(inputVideoPath,firstFramePath);
            Integer sizemax = getSizemaxImage(firstFramePath);
            generateQR(signature, qrCodePath, sizemax);
            addQRVideo(inputVideoPath,qrCodePath,outputVideoPath);

        }catch (Exception e){
            logger.info("Error en el sellado deL video");
            throw e;
        }
    }

    public void cropImage(String inputImagePath,String outputImagePath,boolean videoOrigin){
        try {
            Integer sizemax = getSizemaxImage(inputImagePath);
            int sizeQR = sizemax / 100 * 10; // 10% del tamaño máximo de la imagen
            sizeQR = (sizeQR < 120) ? 120 : sizeQR;
            Imgcodecs Highgui1 = null;
            Mat originalImage = Highgui1.imread(inputImagePath);
            Rect rectCrop = null;
            if (videoOrigin) {
                rectCrop = new Rect(0, 0, sizeQR, sizeQR);
            } else {
                rectCrop = new Rect(0, originalImage.height()-sizeQR, sizeQR, sizeQR);
            }
            Mat croppedImage = new Mat(originalImage, rectCrop);
            Imgcodecs.imwrite(outputImagePath, croppedImage);
        }catch (Exception e){
            logger.info("Error al cortar: " + e.getMessage());
            throw e;
        }
    }

    public void getFirstImageVideo(String inputVideoPath,String outputImagePath) throws Exception {
        try {
            logger.info("getFirstImageVideo:" + inputVideoPath);

            FFmpeg ffmpeg = new FFmpeg(ffmpegPath);
            FFprobe ffprobe = new FFprobe(ffprobePath);

            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(inputVideoPath)
                    .overrideOutputFiles(true)
                    .addOutput(outputImagePath)
                    .setFrames(1)
                    .setFormat("image2")
                    //.setVideoFilter("select='gte(n\\,10)',scale=200:-1")
                    .done();

            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

            // Run a one-pass encode
            executor.createJob(builder).run();

            logger.info("Se obtuvo la imagen en: " + outputImagePath);
        }catch (Exception e){
            logger.info("Error:" + e.getMessage());
            throw new Exception("Error en obtener 1ra imagen de video");
        }
    }

    public void addQRVideo(String inputVideoPath,String qrCodePath,String outputVideoPath) throws Exception {
        try {
            logger.info("addQRVideo:" + inputVideoPath + " - " + qrCodePath);

            FFmpeg ffmpeg = new FFmpeg(ffmpegPath);
            FFprobe ffprobe = new FFprobe(ffprobePath);
            FFmpegBuilder builder = new FFmpegBuilder();
            builder.addInput(inputVideoPath);
            builder.addInput(qrCodePath);
            builder.setComplexFilter("[0:v][1:v]overlay=eof_action=pass:enable='between(t,0,2)'");
            builder.addOutput(outputVideoPath);
            builder.overrideOutputFiles(true);
            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
            // Run a one-pass encode
            executor.createJob(builder).run();

            logger.info("Se sello el video en: " + outputVideoPath);
        }catch (Exception e){
            logger.info("Error:" + e.getMessage());
            throw new Exception("Error en el sellado deL video");
        }
    }

    public void combinePDFAndQR(String inputPDFPath, String qrCodePath, String outputPDFPath) throws IOException{
        try {
            PDDocument pdfDocument = PDDocument.load(new File(inputPDFPath));

            PDImageXObject pdImage = PDImageXObject.createFromFile(qrCodePath, pdfDocument);

            PDPage primeraPagina = pdfDocument.getPage(0);
            PDResources resources = primeraPagina.getResources();
            if (resources == null) {
                resources = new PDResources();
                primeraPagina.setResources(resources);
            }

            PDPageContentStream contentStream = new PDPageContentStream(pdfDocument, primeraPagina, PDPageContentStream.AppendMode.APPEND, true, true);

            float imagenWidth = pdImage.getWidth();
            float imagenHeight = pdImage.getHeight();
            float x = 0; // Posición horizontal de la imagen
            float y = primeraPagina.getBBox().getHeight() - imagenHeight; // Posición vertical de la imagen

            contentStream.drawImage(pdImage, x, y, imagenWidth, imagenHeight);

            contentStream.close();

            pdfDocument.save(outputPDFPath);
            pdfDocument.close();

            System.out.println("Imagen insertada correctamente en la primera página del PDF.");

        } catch (IOException e) {
            logger.info("Error en la combinación de la Imagen y el QR");
            throw e;
        }
    }

    public List<String> getImagesFromPDF(String pathPDF) {
        try {
            PDDocument pdfDocument = PDDocument.load(new File(pathPDF));
            PDPage page = pdfDocument.getPage(0);

            return getImagesFromResources(page.getResources());
        }catch (Exception e){
            return new ArrayList<String>();
        }
    }

    private List<String> getImagesFromResources(PDResources resources) throws IOException {
        List<String> images = new ArrayList<>();
        if(resources==null)
            return  images;

        for (COSName xObjectName : resources.getXObjectNames()) {
            PDXObject xObject = resources.getXObject(xObjectName);

            if (xObject instanceof PDFormXObject) {
                images.addAll(getImagesFromResources(((PDFormXObject) xObject).getResources()));
            } else if (xObject instanceof PDImageXObject) {
                String pathImage = workdir + File.separator + "tmp"+File.separator+UUID.randomUUID().toString() + "_imagePDF.jpg";
                ImageIO.write(((PDImageXObject)xObject).getImage(), "png", new File(pathImage));
                images.add(pathImage);
            }
        }

        return images;
    }
}
