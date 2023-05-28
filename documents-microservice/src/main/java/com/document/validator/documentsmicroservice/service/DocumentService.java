package com.document.validator.documentsmicroservice.service;

import com.document.validator.documentsmicroservice.dto.SingResponseDTO;
import com.document.validator.documentsmicroservice.dto.ValidateResponseDTO;
import com.document.validator.documentsmicroservice.entity.Document;
import com.document.validator.documentsmicroservice.entity.User;
import com.document.validator.documentsmicroservice.repository.UserRepository;
import com.document.validator.documentsmicroservice.repository.DocumentRepository;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import nu.pattern.OpenCV;
import org.apache.commons.io.FileUtils;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
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

    //public String uploadDir = "contenido/tmp/";   //En Google Cloud esta carpeta existe en memoria mientras está en ejeución
    public static String uploadDir = "C:\\Temporal";
    //public static String uploadDir = "c:\\gerardo\\workspace\\Contenido";
    // public static String uploadDir = "Contenido"; //directorio de la aplicación
    //public static String uploadDir = "c:"+ File.separator +"Proyectos"+ File.separator +"Contenido";
    //public String uploadDir = "c:\\Gerardo";
    private  String nameSource = uploadDir;
    private static String uuid;
    private static String fileext;
    private String fileName;
    private static String rutaArchivoOriginalCompress;
    private static String rutaArchivoOriginalQR;

    public SingResponseDTO sign(MultipartFile file, String description, int userId){
        SingResponseDTO responseDTO=new SingResponseDTO();
        try {
            //System.load("C:\\Users\\gavil\\Downloads\\opencv\\opencv\\build\\java\\x64\\opencv_java460.dll");
            //System.loadLibrary("opencv_java460");
            //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            //nu.pattern.OpenCV.loadLocally();
            OpenCV.loadShared();
            System.out.println(Core.VERSION);
            System.out.println("|Sign|Ini."+LocalDateTime.now());
            uuid = UUID.randomUUID().toString();
            fileext = FilenameUtils.getExtension(StringUtils.cleanPath(file.getOriginalFilename())).toLowerCase();
            if (fileext.equals("mp4")){  // Si la extensión es mp4 la cambia a extensión avi
                fileext="avi";
            } else if (fileext.equals("wav")) {
                fileext="mp3";
            }
            System.out.println("|Sign|Type<"+fileext+">."+LocalDateTime.now());
            fileName= uuid + "."+ fileext; //FilenameUtils.getExtension(StringUtils.cleanPath(file.getOriginalFilename()));
            String rutaArchivoFirmado= uploadDir + File.separator + "ImgSealed" + File.separator + fileName;
            String rutaArchivoOriginal=uploadDir + File.separator + "Img" + File.separator + file.getOriginalFilename();
            rutaArchivoOriginalCompress= uploadDir + File.separator + "Img"+ File.separator + "c_"+file.getOriginalFilename();

            nameSource=uploadDir + File.separator + "Img" + File.separator + file.getOriginalFilename();

            Path copyLocation = Paths.get(rutaArchivoFirmado);
            Path copyLocationOri = Paths.get(rutaArchivoOriginal);
            System.out.println(rutaArchivoFirmado);
            System.out.println(rutaArchivoOriginal);
            System.out.println(rutaArchivoOriginalCompress);
            Files.copy(file.getInputStream(), copyLocation, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(file.getInputStream(), copyLocationOri,StandardCopyOption.REPLACE_EXISTING);

            //confirma que el archivo sea gráfico estático para realizar la compresión

            File file2 = new File(rutaArchivoOriginalCompress);
            Document document = new Document();
            document.setFileName(StringUtils.cleanPath(file.getOriginalFilename()));
            document.setDescription(uuid);
            document.setCreatedBy(userId);
            document.setCreatedDate(new Date());
            document.setHashOriginalDocument(generaHash(rutaArchivoFirmado));
            document = documentRepository.save(document);
            // Verificamos si es una imagen estática para agregar código QR
            if(fileext.equals("jpg") || fileext.equals("jpeg") || fileext.equals("png") || fileext.equals("jfif")   ){
                // Comprimir archivo origen
                String dc = rutaArchivoOriginal;
                String dr = rutaArchivoOriginalCompress;
                rutaArchivoOriginalQR=rutaArchivoOriginal;
                File file1 = new File(dc);
                BufferedImage image = ImageIO.read(file1);
                OutputStream os =new FileOutputStream(new File(dr));
                Iterator<ImageWriter> writers =  ImageIO.getImageWritersByFormatName("jpg");
                ImageWriter writer = (ImageWriter) writers.next();

                ImageOutputStream ios = ImageIO.createImageOutputStream(os);
                writer.setOutput(ios);

                ImageWriteParam param = writer.getDefaultWriteParam();

                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                System.out.println("Aplicando compresion 0.99f");
                param.setCompressionQuality(0.99f);
                //param.setCompressionQuality(0.0f);
                writer.write(null, new IIOImage(image, null, null), param);

                os.close();
                ios.close();
                writer.dispose();
                // Compression finished

                // Generated thumbnail
                String imgFile = dc;
                //print(imgFile);
                Mat resizeimage;
                Mat src  =  imread(imgFile);
                System.out.println("Type src.: "+src.type());
                Integer sizemax = (src.cols()>src.rows())?src.cols() : src.rows();
                resizeimage = new Mat();
                Size scaleSize = new Size(200,100);
                resize(src, resizeimage, scaleSize , 0, 0, INTER_AREA);

                Imgcodecs.imwrite(uploadDir + File.separator + "Miniaturas"+ File.separator+"m_"+uuid+".jpg" , resizeimage);
                System.out.println("Generated little image...: "+"m_"+uuid+".jpg");

                // Generated image sealed
                String QRPath;
                // Generate Hash
                String hashcontent = uuid;
                LocalDateTime datetime = LocalDateTime.now();

                String pathQR = uploadDir + File.separator +"QR"+ File.separator;
                String nameQR = uuid+"_CodeQR";
                System.out.println("Ruta QR: "+pathQR);
                System.out.println(userId);
                // QRPath = generateQR("AliPsé sealed|Id="+"IdContent="+generaHash(rutaArchivoFirmado)+"|date="+datetime.toString(),pathQR,nameQR);
                String IdDocumento = String.valueOf(document.getId());
                String Iddatedoc = String.valueOf(document.getCreatedDate());
                String IdCreator = String.valueOf(document.getCreatedBy());
                QRPath = generateQR(String.valueOf(IdDocumento)
                        +"|"+document.getHashOriginalDocument()
                        +"|"+Iddatedoc
                        +"|"+IdCreator
                        ,pathQR,nameQR,sizemax  );

                File fileOrigen = new File(rutaArchivoOriginal);
                BufferedImage imageOrigen = ImageIO.read(fileOrigen);
                System.out.println(QRPath);
                File fileQR = new File(QRPath);
                BufferedImage imageQR = ImageIO.read(fileQR);
                BufferedImage image3 = combineCodeAndBackImage(imageOrigen,imageQR,15,15);

            }else{
                // En caso que no sea una imagen se igualará la variable con el archivo original
                rutaArchivoOriginalCompress = rutaArchivoOriginal;
            }
            rutaArchivoOriginalCompress = rutaArchivoOriginal;

            // Crear un archivo destino a partir de un origen
            // System.out.print("Tamaño del archivo "+rutaArchivoOriginalCompress+" :");
            System.out.print("Tamaño del archivo "+rutaArchivoOriginal+" :");

            int paquetes= (int) (file.getSize()/1024);
            System.out.println(file.getSize());

            //Path pathcompress = Paths.get(rutaArchivoOriginalCompress);
            Path pathcompress = Paths.get(rutaArchivoOriginal);
            System.out.println(Files.size(pathcompress));
            paquetes= (int) (Files.size(pathcompress)/256);

            System.out.println("Paquetes:");
            System.out.print(paquetes);
            // Crear Sello AliPsé
            System.out.print("Sello: AliPse"+uuid);
            String selloAlipse = "";  // textToBinary("AlipSe"+uuid+"ESS--");
            System.out.print("Sello en Binario:");
            Gson gson = new Gson();

            System.out.println("Sin decodificar : " + gson.toJson(document));
            byte[] encodedBytes = Base64.encodeBase64(gson.toJson(document).getBytes());

            System.out.println("encodedBytes " + new String(encodedBytes));

            selloAlipse="--AliPse" + gson.toJson(document) + "EOS--";
            //selloAlipse="--AliPse" + new String(encodedBytes) + "EOS--";

            byte[] decodedBytes = Base64.decodeBase64(encodedBytes);
            System.out.println("decodedBytes " + new String(decodedBytes));

            //Gson gson1 = new Gson();
            //Document document1 = gson1.fromJson(decodedBytes.toString(),Document.class);
            //System.out.print("Nombre del archivo decodificado: " +document1.getFileName());

            Type collectionType = new TypeToken<Collection<Document>>(){}.getType();
            System.out.print("<1>");
            //Collection<Document> enums = gson.fromJson(decodedBytes.toString(), collectionType);
            System.out.print("<2>");
            //System.out.print("enums: " + enums.toString());

            FileOutputStream fos = null;
            DataOutputStream salida = null;

            // Se abre el fichero original para lectura
            FileInputStream fileInput = new FileInputStream(rutaArchivoOriginal);
            BufferedInputStream bufferedInput = new BufferedInputStream(fileInput);


            // Se abre el fichero donde se hará la copia
            //FileOutputStream fileOutput = new FileOutputStream (uploadDir + File.separator +"ejemplo.gif");
            FileWriter fichero = null;
            FileOutputStream fileOutput = new FileOutputStream (rutaArchivoFirmado);
            //fichero = new FileWriter(rutaArchivoFirmado,true);

            BufferedOutputStream bufferedOutput = new BufferedOutputStream(fileOutput);

            // Bucle para leer de un fichero y escribir en el otro.
            byte [] array = new byte[256];
            int leidos = bufferedInput.read(array);
            int veces =1;
            while (leidos > 0)
            {
                bufferedOutput.write(array,0,leidos);
                leidos=bufferedInput.read(array);
                if (veces>paquetes-1){
                    // Insertar sello AliPsé
                    System.out.print("Insertar sello AliPsé");
                    bufferedOutput.write(selloAlipse.getBytes());
                    // Inserta último bloque de bytes
                    bufferedOutput.write(array,0,leidos);
                    leidos=bufferedInput.read(array);
                    break;
                }
                veces++;
            }

            // Cierre de los ficheros
            bufferedInput.close();
            bufferedOutput.close();

            document.setHashSignedDocument(generaHash(rutaArchivoFirmado));
            documentRepository.save(document);

            responseDTO.setFileName(fileName);
            responseDTO.setStatus(0);
            responseDTO.setCodeError("DOCU000");
            responseDTO.setMsgError("OK");

            if(fileext.equals("jpg") || fileext.equals("jpeg") || fileext.equals("png") || fileext.equals("jfif")   ) {
                System.out.println("Elimina archivo sin QR " + rutaArchivoFirmado);
                Path fileminiLocation = Paths.get(rutaArchivoFirmado);
                Files.delete(fileminiLocation); // eliminate sellado sin QR
            }

        } catch (Exception e) {
            e.printStackTrace();
            responseDTO.setStatus(0);
            responseDTO.setCodeError("INTERNAL");
            responseDTO.setMsgError("No se pudo guardar el archivo " + file.getOriginalFilename() + ". ¡Prueba Nuevamente!.  Exception: " + e.getMessage());
        }
        System.out.println("|Sign|Fin."+LocalDateTime.now());
        return responseDTO;
    }

    public static String generateHash() {
        SecureRandom random = new SecureRandom();
        return new BigInteger(130, random).toString(32);
    }

    public static String generateQR(String data, String rutaqr, String nameQR, Integer sizemax) throws IOException, WriterException {
        String datacode = data;
        String path =rutaqr+nameQR+".jpg";
        //int sizeQR = 120;"C:\Program Files\Java\jdk1.8.0_351\bin\java.exe" "-javaagent:C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2022.3.1\lib\idea_rt.jar=50071:C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2022.3.1\bin" -Dfile.encoding=UTF-8 -classpath "C:\Program Files\Java\jdk1.8.0_351\jre\lib\charsets.jar;C:\Program Files\Java\jdk1.8.0_351\jre\lib\deploy.jar;C:\Program Files\Java\jdk1.8.0_351\jre\lib\ext\access-bridge-64.jar;C:\Program Files\Java\jdk1.8.0_351\jre\lib\ext\cldrdata.jar;C:\Program Files\Java\jdk1.8.0_351\jre\lib\ext\dnsns.jar;C:\Program Files\Java\jdk1.8.0_351\jre\lib\ext\jaccess.jar;C:\Program Files\Java\jdk1.8.0_351\jre\lib\ext\jfxrt.jar;C:\Program Files\Java\jdk1.8.0_351\jre\lib\ext\localedata.jar;C:\Program Files\Java\jdk1.8.0_351\jre\lib\ext\nashorn.jar;C:\Program Files\Java\jdk1.8.0_351\jre\lib\ext\sunec.jar;C:\Program Files\Java\jdk1.8.0_351\jre\lib\ext\sunjce_provider.jar;C:\Program Files\Java\jdk1.8.0_351\jre\lib\ext\sunmscapi.jar;C:\Program Files\Java\jdk1.8.0_351\jre\lib\ext\sunpkcs11.jar;C:\Program Files\Java\jdk1.8.0_351\jre\lib\ext\zipfs.jar;C:\Program Files\Java\jdk1.8.0_351\jre\lib\javaws.jar;C:\Program Files\Java\jdk1.8.0_351\jre\lib\jce.jar;C:\Program Files\Java\jdk1.8.0_351\jre\lib\jfr.jar;C:\Program Files\Java\jdk1.8.0_351\jre\lib\jfxswt.jar;C:\Program Files\Java\jdk1.8.0_351\jre\lib\jsse.jar;C:\Program Files\Java\jdk1.8.0_351\jre\lib\management-agent.jar;C:\Program Files\Java\jdk1.8.0_351\jre\lib\plugin.jar;C:\Program Files\Java\jdk1.8.0_351\jre\lib\resources.jar;C:\Program Files\Java\jdk1.8.0_351\jre\lib\rt.jar;C:\Proyectos\documents-microservice\target\classes;C:\libs\opencv\opencv\build\java\opencv-460.jar;C:\libs\ImShow-Java-OpenCV-master\ImShow-Java-OpenCV-master\Imshow.jar;C:\Users\conmak_cloud\.m2\repository\org\springframework\boot\spring-boot-starter-data-jpa\2.7.1\spring-boot-starter-data-jpa-2.7.1.jar;C:\Users\conmak_cloud\.m2\repository\org\springframework\boot\spring-boot-starter-aop\2.7.1\spring-boot-starter-aop-2.7.1.jar;C:\Users\conmak_cloud\.m2\repository\org\springframework\spring-aop\5.3.21\spring-aop-5.3.21.jar;C:\Users\conmak_cloud\.m2\repository\org\aspectj\aspectjweaver\1.9.7\aspectjweaver-1.9.7.jar;C:\Users\conmak_cloud\.m2\repository\org\springframework\boot\spring-boot-starter-jdbc\2.7.1\spring-boot-starter-jdbc-2.7.1.jar;C:\Users\conmak_cloud\.m2\repository\com\zaxxer\HikariCP\4.0.3\HikariCP-4.0.3.jar;C:\Users\conmak_cloud\.m2\repository\org\springframework\spring-jdbc\5.3.21\spring-jdbc-5.3.21.jar;C:\Users\conmak_cloud\.m2\repository\jakarta\transaction\jakarta.transaction-api\1.3.3\jakarta.transaction-api-1.3.3.jar;C:\Users\conmak_cloud\.m2\repository\jakarta\persistence\jakarta.persistence-api\2.2.3\jakarta.persistence-api-2.2.3.jar;C:\Users\conmak_cloud\.m2\repository\org\hibernate\hibernate-core\5.6.9.Final\hibernate-core-5.6.9.Final.jar;C:\Users\conmak_cloud\.m2\repository\org\jboss\logging\jboss-logging\3.4.3.Final\jboss-logging-3.4.3.Final.jar;C:\Users\conmak_cloud\.m2\repository\net\bytebuddy\byte-buddy\1.12.11\byte-buddy-1.12.11.jar;C:\Users\conmak_cloud\.m2\repository\antlr\antlr\2.7.7\antlr-2.7.7.jar;C:\Users\conmak_cloud\.m2\repository\org\jboss\jandex\2.4.2.Final\jandex-2.4.2.Final.jar;C:\Users\conmak_cloud\.m2\repository\com\fasterxml\classmate\1.5.1\classmate-1.5.1.jar;C:\Users\conmak_cloud\.m2\repository\org\hibernate\common\hibernate-commons-annotations\5.1.2.Final\hibernate-commons-annotations-5.1.2.Final.jar;C:\Users\conmak_cloud\.m2\repository\org\glassfish\jaxb\jaxb-runtime\2.3.6\jaxb-runtime-2.3.6.jar;C:\Users\conmak_cloud\.m2\repository\org\glassfish\jaxb\txw2\2.3.6\txw2-2.3.6.jar;C:\Users\conmak_cloud\.m2\repository\com\sun\istack\istack-commons-runtime\3.0.12\istack-commons-runtime-3.0.12.jar;C:\Users\conmak_cloud\.m2\repository\com\sun\activation\jakarta.activation\1.2.2\jakarta.activation-1.2.2.jar;C:\Users\conmak_cloud\.m2\repository\org\springframework\data\spring-data-jpa\2.7.1\spring-data-jpa-2.7.1.jar;C:\Users\conmak_cloud\.m2\repository\org\springframework\data\spring-data-commons\2.7.1\spring-data-commons-2.7.1.jar;C:\Users\conmak_cloud\.m2\repository\org\springframework\spring-orm\5.3.21\spring-orm-5.3.21.jar;C:\Users\conmak_cloud\.m2\repository\org\springframework\spring-context\5.3.21\spring-context-5.3.21.jar;C:\Users\conmak_cloud\.m2\repository\org\springframework\spring-tx\5.3.21\spring-tx-5.3.21.jar;C:\Users\conmak_cloud\.m2\repository\org\springframework\spring-beans\5.3.21\spring-beans-5.3.21.jar;C:\Users\conmak_cloud\.m2\repository\org\springframework\spring-aspects\5.3.21\spring-aspects-5.3.21.jar;C:\Users\conmak_cloud\.m2\repository\org\springframework\boot\spring-boot-starter-web\2.7.1\spring-boot-starter-web-2.7.1.jar;C:\Users\conmak_cloud\.m2\repository\org\springframework\boot\spring-boot-starter\2.7.1\spring-boot-starter-2.7.1.jar;C:\Users\conmak_cloud\.m2\repository\org\springframework\boot\spring-boot\2.7.1\spring-boot-2.7.1.jar;C:\Users\conmak_cloud\.m2\repository\org\springframework\boot\spring-boot-autoconfigure\2.7.1\spring-boot-autoconfigure-2.7.1.jar;C:\Users\conmak_cloud\.m2\repository\org\springframework\boot\spring-boot-starter-logging\2.7.1\spring-boot-starter-logging-2.7.1.jar;C:\Users\conmak_cloud\.m2\repository\ch\qos\logback\logback-classic\1.2.11\logback-classic-1.2.11.jar;C:\Users\conmak_cloud\.m2\repository\ch\qos\logback\logback-core\1.2.11\logback-core-1.2.11.jar;C:\Users\conmak_cloud\.m2\repository\org\apache\logging\log4j\log4j-to-slf4j\2.17.2\log4j-to-slf4j-2.17.2.jar;C:\Users\conmak_cloud\.m2\repository\org\apache\logging\log4j\log4j-api\2.17.2\log4j-api-2.17.2.jar;C:\Users\conmak_cloud\.m2\repository\org\slf4j\jul-to-slf4j\1.7.36\jul-to-slf4j-1.7.36.jar;C:\Users\conmak_cloud\.m2\repository\jakarta\annotation\jakarta.annotation-api\1.3.5\jakarta.annotation-api-1.3.5.jar;C:\Users\conmak_cloud\.m2\repository\org\yaml\snakeyaml\1.30\snakeyaml-1.30.jar;C:\Users\conmak_cloud\.m2\repository\org\springframework\boot\spring-boot-starter-json\2.7.1\spring-boot-starter-json-2.7.1.jar;C:\Users\conmak_cloud\.m2\repository\com\fasterxml\jackson\datatype\jackson-datatype-jdk8\2.13.3\jackson-datatype-jdk8-2.13.3.jar;C:\Users\conmak_cloud\.m2\repository\com\fasterxml\jackson\datatype\jackson-datatype-jsr310\2.13.3\jackson-datatype-jsr310-2.13.3.jar;C:\Users\conmak_cloud\.m2\repository\com\fasterxml\jackson\module\jackson-module-parameter-names\2.13.3\jackson-module-parameter-names-2.13.3.jar;C:\Users\conmak_cloud\.m2\repository\org\springframework\boot\spring-boot-starter-tomcat\2.7.1\spring-boot-starter-tomcat-2.7.1.jar;C:\Users\conmak_cloud\.m2\repository\org\apache\tomcat\embed\tomcat-embed-core\9.0.64\tomcat-embed-core-9.0.64.jar;C:\Users\conmak_cloud\.m2\repository\org\apache\tomcat\embed\tomcat-embed-el\9.0.64\tomcat-embed-el-9.0.64.jar;C:\Users\conmak_cloud\.m2\repository\org\apache\tomcat\embed\tomcat-embed-websocket\9.0.64\tomcat-embed-websocket-9.0.64.jar;C:\Users\conmak_cloud\.m2\repository\org\springframework\spring-web\5.3.21\spring-web-5.3.21.jar;C:\Users\conmak_cloud\.m2\repository\org\springframework\spring-webmvc\5.3.21\spring-webmvc-5.3.21.jar;C:\Users\conmak_cloud\.m2\repository\org\springframework\spring-expression\5.3.21\spring-expression-5.3.21.jar;C:\Users\conmak_cloud\.m2\repository\org\projectlombok\lombok\1.18.24\lombok-1.18.24.jar;C:\Users\conmak_cloud\.m2\repository\jakarta\xml\bind\jakarta.xml.bind-api\2.3.3\jakarta.xml.bind-api-2.3.3.jar;C:\Users\conmak_cloud\.m2\repository\jakarta\activation\jakarta.activation-api\1.2.2\jakarta.activation-api-1.2.2.jar;C:\Users\conmak_cloud\.m2\repository\org\springframework\spring-core\5.3.21\spring-core-5.3.21.jar;C:\Users\conmak_cloud\.m2\repository\org\springframework\spring-jcl\5.3.21\spring-jcl-5.3.21.jar;C:\Users\conmak_cloud\.m2\repository\mysql\mysql-connector-java\8.0.29\mysql-connector-java-8.0.29.jar;C:\Users\conmak_cloud\.m2\repository\commons-codec\commons-codec\1.15\commons-codec-1.15.jar;C:\Users\conmak_cloud\.m2\repository\com\github\albfernandez\juniversalchardet\2.0.0\juniversalchardet-2.0.0.jar;C:\Users\conmak_cloud\.m2\repository\com\google\code\gson\gson\2.8.6\gson-2.8.6.jar;C:\Users\conmak_cloud\.m2\repository\commons-io\commons-io\2.11.0\commons-io-2.11.0.jar;C:\Users\conmak_cloud\.m2\repository\com\google\zxing\javase\3.4.1\javase-3.4.1.jar;C:\Users\conmak_cloud\.m2\repository\com\beust\jcommander\1.78\jcommander-1.78.jar;C:\Users\conmak_cloud\.m2\repository\com\github\jai-imageio\jai-imageio-core\1.4.0\jai-imageio-core-1.4.0.jar;C:\Users\conmak_cloud\.m2\repository\com\google\zxing\core\3.3.0\core-3.3.0.jar;C:\Users\conmak_cloud\.m2\repository\org\openpnp\opencv\3.2.0-0\opencv-3.2.0-0.jar;C:\Users\conmak_cloud\.m2\repository\nu\pattern\opencv\2.4.9-4\opencv-2.4.9-4.jar;C:\Users\conmak_cloud\.m2\repository\org\telegram\telegrambots\5.0.1\telegrambots-5.0.1.jar;C:\Users\conmak_cloud\.m2\repository\org\telegram\telegrambots-meta\5.0.1\telegrambots-meta-5.0.1.jar;C:\Users\conmak_cloud\.m2\repository\com\google\guava\guava\30.0-jre\guava-30.0-jre.jar;C:\Users\conmak_cloud\.m2\repository\com\google\guava\failureaccess\1.0.1\failureaccess-1.0.1.jar;C:\Users\conmak_cloud\.m2\repository\com\google\guava\listenablefuture\9999.0-empty-to-avoid-conflict-with-guava\listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar;C:\Users\conmak_cloud\.m2\repository\com\google\code\findbugs\jsr305\3.0.2\jsr305-3.0.2.jar;C:\Users\conmak_cloud\.m2\repository\org\checkerframework\checker-qual\3.5.0\checker-qual-3.5.0.jar;C:\Users\conmak_cloud\.m2\repository\com\google\errorprone\error_prone_annotations\2.3.4\error_prone_annotations-2.3.4.jar;C:\Users\conmak_cloud\.m2\repository\com\google\j2objc\j2objc-annotations\1.3\j2objc-annotations-1.3.jar;C:\Users\conmak_cloud\.m2\repository\com\fasterxml\jackson\core\jackson-annotations\2.13.3\jackson-annotations-2.13.3.jar;C:\Users\conmak_cloud\.m2\repository\com\fasterxml\jackson\jaxrs\jackson-jaxrs-json-provider\2.13.3\jackson-jaxrs-json-provider-2.13.3.jar;C:\Users\conmak_cloud\.m2\repository\com\fasterxml\jackson\jaxrs\jackson-jaxrs-base\2.13.3\jackson-jaxrs-base-2.13.3.jar;C:\Users\conmak_cloud\.m2\repository\com\fasterxml\jackson\module\jackson-module-jaxb-annotations\2.13.3\jackson-module-jaxb-annotations-2.13.3.jar;C:\Users\conmak_cloud\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.13.3\jackson-core-2.13.3.jar;C:\Users\conmak_cloud\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.13.3\jackson-databind-2.13.3.jar;C:\Users\conmak_cloud\.m2\repository\org\glassfish\jersey\inject\jersey-hk2\2.35\jersey-hk2-2.35.jar;C:\Users\conmak_cloud\.m2\repository\org\glassfish\jersey\core\jersey-common\2.35\jersey-common-2.35.jar;C:\Users\conmak_cloud\.m2\repository\org\glassfish\hk2\osgi-resource-locator\1.0.3\osgi-resource-locator-1.0.3.jar;C:\Users\conmak_cloud\.m2\repository\org\glassfish\hk2\hk2-locator\2.6.1\hk2-locator-2.6.1.jar;C:\Users\conmak_cloud\.m2\repository\org\glassfish\hk2\external\aopalliance-repackaged\2.6.1\aopalliance-repackaged-2.6.1.jar;C:\Users\conmak_cloud\.m2\repository\org\glassfish\hk2\hk2-api\2.6.1\hk2-api-2.6.1.jar;C:\Users\conmak_cloud\.m2\repository\org\glassfish\hk2\hk2-utils\2.6.1\hk2-utils-2.6.1.jar;C:\Users\conmak_cloud\.m2\repository\org\javassist\javassist\3.25.0-GA\javassist-3.25.0-GA.jar;C:\Users\conmak_cloud\.m2\repository\org\glassfish\jersey\media\jersey-media-json-jackson\2.35\jersey-media-json-jackson-2.35.jar;C:\Users\conmak_cloud\.m2\repository\org\glassfish\jersey\ext\jersey-entity-filtering\2.35\jersey-entity-filtering-2.35.jar;C:\Users\conmak_cloud\.m2\repository\org\glassfish\jersey\containers\jersey-container-grizzly2-http\2.35\jersey-container-grizzly2-http-2.35.jar;C:\Users\conmak_cloud\.m2\repository\org\glassfish\hk2\external\jakarta.inject\2.6.1\jakarta.inject-2.6.1.jar;C:\Users\conmak_cloud\.m2\repository\org\glassfish\grizzly\grizzly-http-server\2.4.4\grizzly-http-server-2.4.4.jar;C:\Users\conmak_cloud\.m2\repository\org\glassfish\grizzly\grizzly-http\2.4.4\grizzly-http-2.4.4.jar;C:\Users\conmak_cloud\.m2\repository\org\glassfish\grizzly\grizzly-framework\2.4.4\grizzly-framework-2.4.4.jar;C:\Users\conmak_cloud\.m2\repository\jakarta\ws\rs\jakarta.ws.rs-api\2.1.6\jakarta.ws.rs-api-2.1.6.jar;C:\Users\conmak_cloud\.m2\repository\org\glassfish\jersey\core\jersey-server\2.35\jersey-server-2.35.jar;C:\Users\conmak_cloud\.m2\repository\org\glassfish\jersey\core\jersey-client\2.35\jersey-client-2.35.jar;C:\Users\conmak_cloud\.m2\repository\jakarta\validation\jakarta.validation-api\2.0.2\jakarta.validation-api-2.0.2.jar;C:\Users\conmak_cloud\.m2\repository\org\json\json\20180813\json-20180813.jar;C:\Users\conmak_cloud\.m2\repository\org\apache\httpcomponents\httpclient\4.5.13\httpclient-4.5.13.jar;C:\Users\conmak_cloud\.m2\repository\org\apache\httpcomponents\httpcore\4.4.15\httpcore-4.4.15.jar;C:\Users\conmak_cloud\.m2\repository\org\apache\httpcomponents\httpmime\4.5.13\httpmime-4.5.13.jar;C:\Users\conmak_cloud\.m2\repository\org\slf4j\slf4j-api\1.7.36\slf4j-api-1.7.36.jar" com.document.validator.documentsmicroservice.DocumentsMicroserviceApplication
        int sizeQR = sizemax/100*10; // 10% del tamaño máximo de la imagen
        sizeQR = (sizeQR<120)?120:sizeQR;
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put (EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // Establecer la tasa de tolerancia a fallas al valor predeterminado más alto
        hints.put (EncodeHintType.CHARACTER_SET, "UTF-8"); // La codificación de caracteres es UTF-8
        hints.put (EncodeHintType.MARGIN, 1); // El área en blanco del código QR, el mínimo es 0 y hay bordes blancos, pero es muy pequeño, el mínimo es alrededor del 6%

        BitMatrix matrix=new MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, sizeQR, sizeQR, hints);
        MatrixToImageWriter.writeToPath(matrix,"jpg", Paths.get(path));

        ////
        Mat resizeimage;

        Mat src  =  imread(rutaqr+"logo.jpg");
        resizeimage = new Mat();
        Size scaleSize = new Size(30,30);
        resize(src, resizeimage, scaleSize , 0, 0, INTER_AREA);
        Imgcodecs.imwrite(rutaqr+"logo30x30.jpg", resizeimage);

        BufferedImage background1 = ImageIO.read(new File(rutaqr + nameQR + ".jpg"));
        BufferedImage foreground1 = ImageIO.read(new File(rutaqr + "logo30x30.jpg"));

        BufferedImage bufferedImage1 = new BufferedImage(background1.getWidth(),background1.getHeight(),BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d1 = bufferedImage1.createGraphics();

        g2d1.drawImage(background1, 0, 0,null);

        int x = (background1.getWidth() - foreground1.getWidth()) / 2;
        int y = (background1.getHeight() - foreground1.getHeight()) / 2;
        g2d1.drawImage(foreground1, x, y, null);
        g2d1.dispose();
        ImageIO.write(bufferedImage1,"JPEG", new File(rutaqr + nameQR + ".jpg"));
        ////

        return path;
    }

    private static BufferedImage combineCodeAndBackImage(BufferedImage codeImage, BufferedImage backImage) throws IOException {
        return combineCodeAndBackImage(codeImage, backImage, -1, 100);
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
        TestPane test=  (new TestPane());

        return backImage;
    }

    public static class TestPane extends JPanel {

        private BufferedImage background;
        private BufferedImage foreground;

        private BufferedImage finishimage;
        //File outputfile = new File("C:\\Gerardo\\Workspace\\Contenido\\ImgSealed\\"+uuid+"QR.jpg");
        File outputfile = new File(uploadDir+ File.separator +"ImgSealed"+ File.separator +uuid+"QR.jpg");

        public TestPane() throws IOException {
            try {
                System.out.println("Open images to convinated");
                background = ImageIO.read(new File(rutaArchivoOriginalQR));
                System.out.println("Ruta archivo para QR :"+rutaArchivoOriginalQR);
                System.out.println("Foreground:"+uploadDir+ File.separator+"QR"+File.separator+uuid+"_CodeQR.jpg");
                foreground = ImageIO.read(new File(uploadDir+ File.separator+"QR"+File.separator+uuid+"_CodeQR.jpg"));
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
            Path fileQRLocation = Paths.get(uploadDir+ File.separator+"QR"+File.separator+uuid+"_CodeQR.jpg" );
            Files.delete(fileQRLocation); // eliminate temp differ
            System.out.println("Elimina el archivo QR 11: ");
         }
    }

    public ValidateResponseDTO validate(MultipartFile file){
        ValidateResponseDTO responseDTO=new ValidateResponseDTO();
        try {
            //System.load("C:\\Users\\gavil\\Downloads\\opencv\\opencv\\build\\java\\x64\\opencv_java460.dll");
            //System.out.println(System.getProperty("java.library.path"));
            OpenCV.loadShared();
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
                        if (!documentBD.getHashSignedDocument().equals(generaHash(rutaArchivoFirmado))&&resQR=="") {
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
                            double resCompare= compareContentQR(documentBD.getDescription(),rutaArchivoFirmado);
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
            rutaArchivoMini = uploadDir + File.separator + "Miniaturas"+ File.separator+"m_"+document.getDescription()+".jpg";
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

    private double compareContentQR(String description, String rutaArchivoFirmado) throws IOException {

        String rutaArchivoQR = uploadDir +File.separator +"ImgSealed"+File.separator +description+"QR.jpg";
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
            Highgui1.imwrite(uploadDir+ File.separator+ "Img"+ File.separator +description+"_redim.jpg",img222);
            rutaArchivoFirmado = uploadDir+File.separator+ "Img"+ File.separator +description+"_redim.jpg";
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
            Highgui1.imwrite(uploadDir+File.separator+"Img"+File.separator+description+"_redim_fake.jpg",src2);
            rutaArchivoFirmado = uploadDir+File.separator+"Img"+File.separator+description+"_redim_fake.jpg";
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
    public static String generaHash(String rutaArchivo)
    {
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
