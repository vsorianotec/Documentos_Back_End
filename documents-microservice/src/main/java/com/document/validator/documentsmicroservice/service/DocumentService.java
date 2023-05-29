package com.document.validator.documentsmicroservice.service;

import com.document.validator.documentsmicroservice.controller.dto.SingResponseDTO;
import com.document.validator.documentsmicroservice.controller.dto.ValidateResponseDTO;
import com.document.validator.documentsmicroservice.entity.Document;
import com.document.validator.documentsmicroservice.entity.User;
import com.document.validator.documentsmicroservice.repository.UserRepository;
import com.document.validator.documentsmicroservice.repository.DocumentRepository;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import nu.pattern.OpenCV;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import org.apache.commons.io.FileUtils;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
//import org.opencv.objdetect.QRCodeDetector;
import com.google.zxing.common.HybridBinarizer;

import org.opencv.objdetect.QRCodeDetector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.codec.binary.Base64;

import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgproc.Imgproc.*;

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
        try {
            //System.load("C:\\Users\\gavil\\Downloads\\opencv\\opencv\\build\\java\\x64\\opencv_java460.dll");
            //System.out.println(System.getProperty("java.library.path"));
            //OpenCV.loadShared();
            //System.load("opencv_java460.dll");
            //System.out.println(Core.VERSION);
            //nu.pattern.OpenCV.loadLocally();
            System.out.println(Core.VERSION);
            System.out.println("|Val|Ini."+LocalDateTime.now());

            uuid = UUID.randomUUID().toString(); //Generate session uuid

            String uuid = UUID.randomUUID().toString();
            String fileName= uuid + "."+ FilenameUtils.getExtension(StringUtils.cleanPath(file.getOriginalFilename()));
            String rutaArchivoFirmado= uploadDir + File.separator + "tmp"+File.separator+"Sellado"+fileName;
            Path copyLocation = Paths.get(rutaArchivoFirmado);
            Files.copy(file.getInputStream(), copyLocation, StandardCopyOption.REPLACE_EXISTING);
            fileext = FilenameUtils.getExtension(StringUtils.cleanPath(fileName).toLowerCase());
            System.out.println(fileext);

            FileReader archivo = null;
            InputStreamReader fr = null;
            BufferedReader br = null;
            Document documentBD = null;
            User user= null;
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
                    //System.out.println(linea);
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
                System.out.println("Validando firma AliPse");
                System.out.println("Firma:");
                System.out.print(firma);
                System.out.println(fin);
                br.close();
                String resQR="";
                Boolean findMiniContent=false;
                if(firma<0 || fin <0 ){
                    if(fileext.equals("jpg") || fileext.equals("jpeg") || fileext.equals("png") || fileext.equals("jfif") )  {
                        //Verify if exist code QR
                        resQR= veirifyImageQR(rutaArchivoFirmado);
                        System.out.println("Response QR:<"+resQR+"> Length:"+resQR.length());
                        if (resQR=="" || resQR.length()==0) {
                            System.out.println("Busca en miniaturas..1");
                            //Buscar miniatura
                            String imgFile = rutaArchivoFirmado;
                            Mat resizeimage;
                            Mat src  =  imread(imgFile);
                            resizeimage = new Mat();
                            Size scaleSize = new Size(200,100);
                            resize(src, resizeimage, scaleSize , 0, 0, INTER_AREA);
                            Imgcodecs.imwrite(uploadDir + File.separator + "Miniaturas"+ File.separator+"mtmp_"+fileName , resizeimage);
                            System.out.println("Generated little image to find ...");
                            String[] resminiMatch = compareMinis("mtmp_"+fileName);

                            Path fileminiLocation = Paths.get(uploadDir + File.separator + "Miniaturas"+ File.separator+"mtmp_"+fileName );
                            Files.delete(fileminiLocation); // eliminate temp mini "mtmp"

                            Double resmini = Double.valueOf(resminiMatch[0]);
                            System.out.println("Resultado: "+(100-resmini));

                            if(100 - resmini>99.69){
                                responseDTO.setDocumentId(Integer.parseInt(resminiMatch[1]));
                                Date createdDate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(resminiMatch[3]);
                                responseDTO.setCreatedDate(createdDate);
                                responseDTO.setOriginalName(resminiMatch[4]);
                                responseDTO.setAuthor(resminiMatch[5]);
                                responseDTO.setEmail(resminiMatch[6]);

                                responseDTO.setStatus(0);
                                responseDTO.setCodeError("DOCU000");
                                responseDTO.setMsgError("OK");
                                //Path fileminiLocation = Paths.get(uploadDir + File.separator + "Miniaturas"+ File.separator+"mtmp_"+fileName );
                                //Files.delete(fileminiLocation); // eliminate temp mini "mtmp"
                                System.out.println("Elimina el archivo Fake 8: <"+copyLocation+">");
                                Files.delete(copyLocation);
                                System.out.println("|Val|FinMiniOK."+LocalDateTime.now());
                                return responseDTO;
                            } else if (100 - resmini>98) {
                                responseDTO.setStatus(1);
                                responseDTO.setCodeError("DOCU004");
                                responseDTO.setMsgError("Not an Alipsé Sealed File, yet it looks VERY MUCH LIKE\n one of the images in our database by [author] (please be warned it’s not identical)"); //El documento contiene una firma no reconocida
                                System.out.print("Fin Null . " + LocalDateTime.now());
                                //Path fileminiLocation = Paths.get(uploadDir + File.separator + "Miniaturas"+ File.separator+"mtmp_"+fileName );
                                //Files.delete(fileminiLocation); // eliminate temp mini "mtmp"
                                System.out.println("Elimina el archivo Fake 7: <"+copyLocation+">");
                                Files.delete(copyLocation);
                                System.out.println("|Val|FinMiniFake."+LocalDateTime.now());
                                return responseDTO;
                            } else {
                                responseDTO.setStatus(1);
                                responseDTO.setCodeError("DOCU004");
                                responseDTO.setMsgError("Alipsé Sealed FAKE file\n [the author] invests to avoid impersonation"); //El documento contiene una firma no reconocida
                                System.out.print("Fin Null . " + LocalDateTime.now());
                                //Path fileminiLocation = Paths.get(uploadDir + File.separator + "Miniaturas"+ File.separator+"mtmp_"+fileName );
                                //Files.delete(fileminiLocation); // eliminate temp mini "mtmp"
                                System.out.println("Elimina el archivo Fake : <"+copyLocation+">");
                                Files.delete(copyLocation);
                                System.out.println("|Val|FinMiniNotSaled."+LocalDateTime.now());
                                return responseDTO;
                            }
                        } else {
                            System.out.println("The image contains a QR Code");
                        }
                    }else{
                        responseDTO.setStatus(1);
                        responseDTO.setCodeError("DOCU001");
                        responseDTO.setMsgError("Alipsé Sealed FAKE file\n[the author] invests to avoid impersonation");
                        //Path fileminiLocation = Paths.get(uploadDir + File.separator + "Miniaturas"+ File.separator+"mtmp_"+fileName );
                        //Files.delete(fileminiLocation); // eliminate temp mini "mtmp"
                        System.out.println("Elimina el archivo Fake 5: <"+copyLocation+">");
                        Files.delete(copyLocation);
                        System.out.println("|Val|FinSinQRFake."+LocalDateTime.now());
                        return responseDTO;
                    }

                }
                Gson gson = new Gson();
                Document document = new Document();

                if(resQR=="" && !findMiniContent) { //Does not exist code QR
                    System.out.println("inicio: " + firma);
                    System.out.println("fin: " + fin);
                    String json = lastLine.substring(firma + 8, fin); // gets signed
                    System.out.println("json: " + json);
                    byte[] decodedBytes = Base64.decodeBase64(json);
                    System.out.println("decodedBytes " + new String(decodedBytes));
                    //json=decodedBytes.toString();
                    System.out.print("Inicia . " + LocalDateTime.now());
                    document = gson.fromJson(json, Document.class);
                    System.out.println("<1> . " + LocalDateTime.now());
                }else if (resQR.length()>0)  { //Exist code QR
                    System.out.println("QR=<"+resQR+">");
                    String[] parts = resQR.split("\\|"); //Result convert in matrix and set in document object
                     //document=null;
                    if(parts.length>0) {
                        //document = gson.fromJson(json, Document.class);
                        document.setId(Integer.parseInt(parts[0]));
                        document.setHashOriginalDocument(parts[1]);
                        document.setCreatedBy(Integer.parseInt(parts[3]));
                    }
                }
                if(document==null){
                    responseDTO.setStatus(1);
                    responseDTO.setCodeError("DOCU002");
                    responseDTO.setMsgError("Alipsé Sealed FAKE file\n[the author] invests to avoid impersonation");
                    System.out.println("<2> . "+ LocalDateTime.now());
                    //Path fileminiLocation = Paths.get(uploadDir + File.separator + "Miniaturas"+ File.separator+"mtmp_"+fileName );
                    //Files.delete(fileminiLocation); // eliminate temp mini "mtmp"
                    System.out.println("Elimina el archivo Fake 4: <"+copyLocation+">");
                    Files.delete(copyLocation);
                    System.out.println("|Val|SinQRNotSaledFinFake."+LocalDateTime.now());
                    return responseDTO;
                }else{
                    System.out.println("<3> . "+ LocalDateTime.now());
                    System.out.println("Valido . "+ LocalDateTime.now());
                    documentBD = documentRepository.findById(document.getId()).orElse(null);
                    if(documentBD!=null) {
                        user = userRepository.getReferenceById(document.getCreatedBy());
                        System.out.println("BuscaHash . "+ LocalDateTime.now());
                        System.out.println("File signed: "+rutaArchivoFirmado);
                        responseDTO.setDocumentId(documentBD.getId());
                        responseDTO.setCreatedDate(documentBD.getCreatedDate());
                        responseDTO.setOriginalName(documentBD.getFileName());
                        responseDTO.setAuthor(user.getName());
                        responseDTO.setEmail(user.getEmail());
                        if (!documentBD.getHashSignedDocument().equals(fileService.generateHash(rutaArchivoFirmado))&&resQR=="") {
                            responseDTO.setStatus(1);
                            responseDTO.setCodeError("DOCU002");
                            responseDTO.setMsgError("Not an Alipsé Sealed File, yet it looks VERY MUCH LIKE\n one of the images in our database by [author] (please be warned it’s not identical)");
                            System.out.println("FinBuscaHash . "+ LocalDateTime.now());
                            //Path fileminiLocation = Paths.get(uploadDir + File.separator + "Miniaturas"+ File.separator+"mtmp_"+fileName );
                            //Files.delete(fileminiLocation); // eliminate temp mini "mtmp"
                            System.out.println("Elimina el archivo Fake QR 3: <"+copyLocation+">");
                            Files.delete(copyLocation);
                            System.out.println("|Val|SinQRFinFake."+LocalDateTime.now());
                            return responseDTO;
                        }if(resQR!=""){ //Find the image with OpenCV

                            System.out.println("<4>Find with OpenCV . "+ LocalDateTime.now());
                            System.out.println("Begin comparing...");
                            double resCompare= compareContentQR(documentBD.getUuid(),rutaArchivoFirmado);
                            System.out.println(100-resCompare);
                            System.out.println("Finish comparing...");
                            if(100 - resCompare>99.99){
                                // 2% umbral corrección

                            } else if (100 - resCompare>98) {
                                responseDTO.setStatus(1);
                                responseDTO.setCodeError("DOCU004");
                                responseDTO.setMsgError("Not an Alipsé Sealed File, yet it looks VERY MUCH LIKE\none of the images in our database by [author] (please be warned it’s not identical) "); //El documento contiene una firma no reconocida
                                System.out.print("Fin Null . " + LocalDateTime.now());
                                //Path fileminiLocation = Paths.get(uploadDir + File.separator + "Miniaturas"+ File.separator+"mtmp_"+fileName );
                                //Files.delete(fileminiLocation); // eliminate temp mini "mtmp"
                                System.out.println("Elimina el archivo Fake QR 3: <"+copyLocation+">");
                                Files.delete(copyLocation);
                                System.out.println("|Val|SinQRcomparingFinFake."+LocalDateTime.now());
                                return responseDTO;
                            } else {
                                responseDTO.setStatus(1);
                                responseDTO.setCodeError("DOCU004");
                                responseDTO.setMsgError("Not an Alipsé Sealed File, we cannot determine its authenticity"); //El documento contiene una firma no reconocida
                                System.out.print("Fin Null . " + LocalDateTime.now());
                                //Path fileminiLocation = Paths.get(uploadDir + File.separator + "Miniaturas"+ File.separator+"mtmp_"+fileName );
                                //Files.delete(fileminiLocation); // eliminate temp mini "mtmp"
                                System.out.println("Elimina el archivo Fake QR 2: <"+copyLocation+">");
                                Files.delete(copyLocation);
                                System.out.println("|Val|SinQRComparingNotSaledFinFake."+LocalDateTime.now());
                                return responseDTO;
                            }
                        }
                    }else{
                        responseDTO.setStatus(1);
                        responseDTO.setCodeError("DOCU002");
                        responseDTO.setMsgError("Alipsé Sealed FAKE file\n[the author] invests to avoid impersonation");
                        System.out.println("<2> . "+ LocalDateTime.now());
                        //Path fileminiLocation = Paths.get(uploadDir + File.separator + "Miniaturas"+ File.separator+"mtmp_"+fileName );
                        //Files.delete(fileminiLocation); // eliminate temp mini "mtmp"
                        System.out.println("Elimina el archivo Fake 4: <"+copyLocation+">");
                        Files.delete(copyLocation);
                        System.out.println("|Val|SinQRNotSaledFinFake."+LocalDateTime.now());
                        return responseDTO;
                    }
                }


            }catch(Exception e){
                e.printStackTrace();
                responseDTO.setStatus(0);
                responseDTO.setCodeError("INTERNAL");
                responseDTO.setMsgError("Could not store file " + file.getOriginalFilename() + ". Please try again!. Exception: " + e.getMessage());
                //Path fileminiLocation = Paths.get(uploadDir + File.separator + "Miniaturas"+ File.separator+"mtmp_"+fileName );
                //Files.delete(fileminiLocation); // eliminate temp mini "mtmp"
                System.out.println("Elimina el archivo Error 1: <"+copyLocation+">");
                Files.delete(copyLocation);

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

            //Path fileminiLocation = Paths.get(uploadDir + File.separator + "Miniaturas"+ File.separator+"mtmp_"+fileName );
            //Files.delete(fileminiLocation); // eliminate temp mini "mtmp"
            System.out.println("Elimina el archivo : <"+copyLocation+">");
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
        System.out.println("|Val|FinOK."+LocalDateTime.now());
        return responseDTO;
    }

    private String[] compareMinis(String fileMini) throws IOException {
        System.out.println("Count repository: "+documentRepository.count());
        List<Document> documents = documentRepository.findAll();
        String rutaArchivoMini = "";
        String[] resBDdMini = new String[]{"","","","","","",""};
        Double resBestMatchMini = 200.0,resMatchMini=0.0;
        for(Document document : documents){
            rutaArchivoMini = uploadDir + File.separator + "Miniaturas"+ File.separator+"m_"+document.getUuid()+".jpg";
            System.out.print("Validando existencia: "+rutaArchivoMini);
            File fileMiniInBD = new File(rutaArchivoMini);
            // Checking if the specified file exists or not
            resMatchMini=200.0;
            if (fileMiniInBD.exists()) {
                System.out.println(" Exists");
                resMatchMini = compareMinisProcess(fileMini,rutaArchivoMini);
            }else System.out.println(" Does not Exists");
            System.out.println("resMatchMini: "+resMatchMini);
            System.out.println("resBestMatchMini: "+resBestMatchMini);
            if(resMatchMini<resBestMatchMini){
                resBestMatchMini = resMatchMini;
                resBDdMini[0]= String.valueOf(resMatchMini);
                resBDdMini[1]=String.valueOf(document.getId());
                resBDdMini[2]=String.valueOf(document.getCreatedBy());
                resBDdMini[3]=document.getCreatedDate().toString();
                resBDdMini[4]=document.getFileName();
                System.out.println("Mejor ... : "+resBestMatchMini);
            }
        }
        System.out.println("Mejor coincidencia: "+resBestMatchMini);
        if(100 - resBestMatchMini>99.69 ){
            Optional<User> userBDMatchMini = userRepository.findById(Integer.valueOf(resBDdMini[2]));
            if(userBDMatchMini!=null){
                System.out.println(userBDMatchMini.get().getEmail());
                resBDdMini[5]=userBDMatchMini.get().getName();
                resBDdMini[6]=userBDMatchMini.get().getEmail();
            }
        }else if(100 -resBestMatchMini <99.9 && 100 -resBestMatchMini >98.9){ // && 100 -resBestMatchMini >50.0){
            File source = new File(uploadDir + File.separator+"img"+ File.separator+"partialfake.jpg");
            File dest = new File(uploadDir + File.separator+"tmp"+File.separator+ uuid + "_differeFake.jpg");
            FileUtils.copyFile(source, dest);
        }else if(100 -resBestMatchMini <98.9){ // && 100 -resBestMatchMini >50.0){
            File source = new File(uploadDir + File.separator+"img"+File.separator+"fake.jpg");
            File dest = new File(uploadDir + File.separator+"tmp"+File.separator + uuid + "_differeFake.jpg");
            FileUtils.copyFile(source, dest);
        }
        resBDdMini[0]=String.valueOf(resBestMatchMini);
        return resBDdMini;
    }

    private double compareMinisProcess(String fileMini, String rutaArchivoMini) throws IOException {
        Mat imgs = new Mat();
        Mat erodeImg = new Mat();
        Mat dilateImg = new Mat();
        Mat threshImg = new Mat();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        Mat hierarchy = new Mat();

        Imgcodecs Highgui1 = null;
        String rutaArchivoMiniUpload = uploadDir + File.separator + "Miniaturas"+ File.separator+fileMini;
        System.out.println("Validar:"+fileMini);
        System.out.println("Ruta archivos minis: " +rutaArchivoMini);


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
        System.out.println("Contornos diferentes: "+contours.size());

        List<Rect> boundRect = new ArrayList<Rect>(contours.size());
        String[] size = null;
        double  areaModif = 0;
        for(int i=0;i<contours.size();i++){
//        	Mat conMat = (Mat)contours.get(i);
//        	Imgproc.approxPolyDP((MatOfPoint2f)conMat,contours_poly.get(i),3,true);
            // Genera un rectángulo envolvente externo basado en el contorno
            Rect rect = Imgproc.boundingRect(contours.get(i));
            boundRect.add(rect);
            System.out.println("Rectangle : " + rect+" Size: "+rect.size().toString());
            size=rect.size().toString().split("x");
            System.out.println("Lado:"+size[0]+" Alto:"+size[1]);
            areaModif=areaModif+(Double.parseDouble(size[0]) * Double.parseDouble(size[1]));
        }
        System.out.println("Area modificada: "+areaModif);
        System.out.println("Area de la imagen: "+img1.rows()*img1.cols());
        System.out.println("% "+ (areaModif/(img1.rows()*img1.cols()))*100);
        for(int i=0;i<contours.size();i++){
            Scalar color = new Scalar(0,0,255);
            // Dibujar contorno
            //Imgproc.drawContours(img222, contours, i, color, 1, Core.LINE_8, hierarchy, 0, new Point());
            // Dibujar rectángulo
            Imgproc.rectangle(img2, boundRect.get(i).tl(), boundRect.get(i).br(), color, 2, Imgproc.LINE_8, 0);

        }
        //Highgui1.imwrite("C:\\Users\\gavil\\Pictures\\Sellada_rect1.jpg", img111);
        Highgui1.imwrite(uploadDir + File.separator+"tmp" +File.separator+ uuid + "_differe.jpg", img2);
        System.out.println("ruta dif: "+uploadDir + File.separator+"tmp" +File.separator+ uuid + "_differe.jpg");

        // Highgui.imwrite(uploadDir+"\\Img\\"+description+"_diff.jpg",subtractResult);
        // Reglas de negocio
        double differ = (areaModif/(img1.rows()*img1.cols()))*100;
        System.out.println(differ);

        Path filedifferLocation = Paths.get(uploadDir + File.separator+"tmp"+File.separator + uuid + "_differe.jpg" );
        Files.delete(filedifferLocation); // eliminate temp differ
        System.out.println("Elimina el archivo Differ 10: ");


        return differ;
    }

    private double compareContentQR(String uuid, String rutaArchivoFirmado) throws IOException {

        String rutaArchivoQR = uploadDir +File.separator +"ImgSealed"+File.separator +uuid+".jpg";
        // rutaArchivoQR = uploadDir +"\\ImgSealed\\188882cf-de91-47fe-aca3-5e669daadd35QR.jpg";
        System.out.println(rutaArchivoQR + "-" + rutaArchivoFirmado);

        //Metodo escala de grises

        Imgcodecs Highgui1 = null;
        Mat img111 = Highgui1.imread(rutaArchivoQR);
        Mat img222 = Highgui1.imread(rutaArchivoFirmado);
        //img222 = Highgui1.imread(uploadDir+"\\Img\\"+description+"_redim.jpg");

        if(img111.rows()!= img222.rows() || img111.cols()!=img222.cols()){ // Las imagenes tienen tamaños diferentes se aplica resize
            Mat resizeimage = new Mat();
            Size scaleSize = new Size(img111.cols(),img111.rows());
            resize(img222, img222, scaleSize , 0, 0, INTER_AREA);
            Highgui1.imwrite(uploadDir+ File.separator+ "Img"+ File.separator +uuid+"_redim.jpg",img222);
            rutaArchivoFirmado = uploadDir+File.separator+ "Img"+ File.separator +uuid+"_redim.jpg";
            System.out.println("Redim " +img222.size());
        }

        Mat img = new Mat();
        // Los píxeles son pobres
        Core.absdiff(img111, img222, img);
        // Highgui1.imwrite("C:\\Users\\gavil\\Pictures\\Sellada_salida.jpg", img);
        Highgui1.imwrite(uploadDir +File.separator+"tmp"+File.separator+uuid+"_diff.jpg", img);

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
        System.out.println("Contornos diferentes: "+contours.size());
        List<Rect> boundRect = new ArrayList<Rect>(contours.size());
        String[] size = null;
        double  areaModif = 0;
        for(int i=0;i<contours.size();i++){
//        	Mat conMat = (Mat)contours.get(i);
//        	Imgproc.approxPolyDP((MatOfPoint2f)conMat,contours_poly.get(i),3,true);
            // Genera un rectángulo envolvente externo basado en el contorno
            Rect rect = Imgproc.boundingRect(contours.get(i));
            boundRect.add(rect);
            System.out.println("Rectangle : " + rect+" Size: "+rect.size().toString());
            size=rect.size().toString().split("x");
            System.out.println("Lado:"+size[0]+" Alto:"+size[1]);
            areaModif=areaModif+(Double.parseDouble(size[0]) * Double.parseDouble(size[1]));
        }
        System.out.println("Area modificada: "+areaModif);
        System.out.println("Area de la imagen: "+img111.rows()*img111.cols());
        System.out.println("% "+ (areaModif/(img111.rows()*img111.cols()))*100);
        for(int i=0;i<contours.size();i++){
            Scalar color = new Scalar(0,0,255);
            // Dibujar contorno
        	//Imgproc.drawContours(img222, contours, i, color, 1, Core.LINE_8, hierarchy, 0, new Point());
            // Dibujar rectángulo
            Imgproc.rectangle(img222, boundRect.get(i).tl(), boundRect.get(i).br(), color, 2, Imgproc.LINE_8, 0);

        }
        //Highgui1.imwrite("C:\\Users\\gavil\\Pictures\\Sellada_rect1.jpg", img111);
        Highgui1.imwrite(uploadDir + File.separator+"tmp" +File.separator+ uuid + "_differe.jpg", img222);

        // Highgui.imwrite(uploadDir+"\\Img\\"+description+"_diff.jpg",subtractResult);
        // Reglas de negocio
        double differ = (areaModif/(img111.rows()*img111.cols()))*100;
        System.out.println(differ);

        double alfa=0.7,beta;
        Mat src1,src2,dst;
        URL img1_url,img2_url;
        String ruta1=uploadDir + File.separator+"tmp"+File.separator+ uuid + "_differe.jpg",
               ruta2=uploadDir+File.separator+"tmp"+File.separator+"fake_size.jpg";
        src1 = Highgui1.imread(ruta1);
        src2 = Highgui1.imread(ruta2);

        if( src1.empty() ) { System.out.println("Error Cargando imagen1 n");}
        if( src2.empty() ) { System.out.println("Error Cargando imagen2 n");}

        if(src1.rows()!= src2.rows() || src1.cols()!=src2.cols()){ // Las imagenes tienen tamaños diferentes se aplica resize
            Mat resizeimage = new Mat();
            Size scaleSize = new Size(src1.cols(),src1.rows());
            resize(src2, src2, scaleSize , 0, 0, INTER_AREA);
            Highgui1.imwrite(uploadDir+File.separator+"Img"+File.separator+uuid+"_redim_fake.jpg",src2);
            rutaArchivoFirmado = uploadDir+File.separator+"Img"+File.separator+uuid+"_redim_fake.jpg";
            System.out.println("Redim " +src2.size());
        }
        dst = new Mat();
        beta = 1.0 - alfa;
        Core.addWeighted(src1, alfa, src2, beta, 0.0, dst);
        Highgui1.imwrite( uploadDir + File.separator+"tmp"+File.separator+ uuid + "_differeFake.jpg", dst);

        Path filedifferLocation = Paths.get(uploadDir + File.separator+"tmp"+File.separator + uuid + "_differe.jpg" );
        Files.delete(filedifferLocation); // eliminate temp differ
        System.out.println("Elimina el archivo Differ 10 ");
        Path filedifLocation = Paths.get(uploadDir + File.separator+"tmp"+File.separator + uuid + "_diff.jpg" );
        Files.delete(filedifLocation); // eliminate temp differ
        System.out.println("Elimina el archivo Differ 11 ");



        return differ;
    }
    public static String veirifyImageQR(String pathfile) {
        String res = "";
        try {



            // String pathfile = "C:\\Gerardo\\Workspace\\Contenido\\ImgSealed\\d28ade1d-c73b-49a4-b116-090cd55bff4fQR.jpg"; // Con QR
            //pathfile = "C:\\Gerardo\\Workspace\\Contenido\\ImgSealed\\d28ade1d-c73b-49a4-b116-090cd55bff4fQR.jpg"; // Sin QR
            System.out.println(pathfile);
            Mat img = Imgcodecs.imread(pathfile);
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
        return res;
    }


    public void download(String fileName,HttpServletResponse response){
        try {
            if(fileext.equals("jpg") || fileext.equals("jpeg") || fileext.equals("png") || fileext.equals("jfif") )   {
                File file = new File(uploadDir + File.separator + "ImgSealed" + File.separator + uuid + "QR.jpg");
                if (file.exists()) {
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
                    // file.delete();
                }
                file = new File(uploadDir + File.separator + "tmp" + File.separator + uuid + "_differeFake.jpg");
                if (file.exists()) {
                    //get the mimetype
                    String mimeType = URLConnection.guessContentTypeFromName(file.getName());
                    if (mimeType == null) {
                        //unknown mimetype so set the mimetype to application/octet-stream
                        mimeType = "application/octet-stream";
                    }
                    response.setContentType(mimeType);
                    response.setHeader("Content-Disposition", String.format("inline; filename=\"" + file.getName() + "\""));
                    //Here we have mentioned it to show as attachment
                    //response.setHeader("Content-Disposition", String.format("attachment; filename=\"" + file.getName() + "\""));
                    response.setContentLength((int) file.length());
                    InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
                    FileCopyUtils.copy(inputStream, response.getOutputStream());
                    // file.delete();
                }
                file = new File(uploadDir + File.separator + "tmp" + File.separator + uuid + "_differeFake.jpg");
                if (file.exists()) {
                    //get the mimetype
                    String mimeType = URLConnection.guessContentTypeFromName(file.getName());
                    if (mimeType == null) {
                        //unknown mimetype so set the mimetype to application/octet-stream
                        mimeType = "application/octet-stream";
                    }
                    response.setContentType(mimeType);
                    response.setHeader("Content-Disposition", String.format("inline; filename=\"" + file.getName() + "\""));
                    //Here we have mentioned it to show as attachment
                    //response.setHeader("Content-Disposition", String.format("attachment; filename=\"" + file.getName() + "\""));
                    response.setContentLength((int) file.length());
                    InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
                    FileCopyUtils.copy(inputStream, response.getOutputStream());
                    // file.delete();
                }
            }else{
                File file = new File(uploadDir + File.separator + "ImgSealed" + File.separator + uuid + "."+fileext);
                String mimeType = URLConnection.guessContentTypeFromName(file.getName());
                if(!file.exists()) {
                    file = new File(uploadDir + File.separator + "img" + File.separator + "fake.jpg");
                    mimeType = URLConnection.guessContentTypeFromName(file.getName());
                }
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
                // file.delete();

            }
        }catch (Exception e){
            response.setStatus(400);
        }
    }
}
