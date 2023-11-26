const { Client, LocalAuth } = require('whatsapp-web.js');
const qrcode = require('qrcode-terminal');
const readable = require('stream').Readable;
const fs = require("fs");
const axios = require('axios');
const FormData = require('form-data');
const { v4: uuidv4 } = require('uuid');
const { Console } = require('console');
//const INPUT_PATH = 'C:/app/alipse/telegramIn/';
//const OUTPUT_PATH = 'C:/app/alipse/telegramOut/';
var propertiesReader = require('properties-reader');
var properties = propertiesReader('./app.properties');
var urlValidateService = properties.get('valideServiceURL');

const client = new Client({
    authStrategy: new LocalAuth()
});

client.on('qr', qr => {
    qrcode.generate(qr, {small: true});
});

client.on('ready', () => {
    console.log('Client is ready!');
});

client.on('message', async msg => {
    //console.log (msg);
    if(msg.hasMedia) {
        const media = await msg.downloadMedia();
        console.log('media.mimetype: ' + media.mimetype);
        console.log('media.filename: ' + media.filename);
        if(media.mimetype.includes("image") || media.mimetype.includes("pdf")){
            var type = "";
            if(media.mimetype.includes("image")){
                type="image"
            }
            if(media.mimetype.includes("pdf")){
                type="pdf"
            }

            // do something with the media data here
            let uuid = uuidv4();
            let filename = uuid + "." + media.mimetype.split("/")[1];

            const fileBuffer = Buffer.from(media.data,'base64');
           
            // Guardado del archivo:
            //const stream = new readable();
            //stream.push(fileBuffer);
            //stream.push(null);
            //stream.pipe(fs.createWriteStream(INPUT_PATH + filename)); // Here I know the file type so added .pdf as extension.

            client.sendMessage(msg.from, 'You have just uploaded a ' + type + ', its authenticity will be validated, please wait while it finishes...');

            // Create a form and append image with additional fields
            const form = new FormData();
            form.append('file', fileBuffer, filename);

            // Send form data with axios
            const response = await axios.post(urlValidateService, form, {
                headers: {
                    ...form.getHeaders()
                }
            });

            if(response.data.status==0 && response.data.codeError=='DOCU000'){
                msg.reply('Alipsé Sealed TRUE ' + type +'\n[' + response.data.author +'] invests to avoid impersonation\nVerify By Alipsé');
            }else if(response.data.status==1 && (response.data.codeError=='DOCU004' || response.data.codeError=='DOCU005')){
                msg.reply('Not an Alipsé Sealed ' + type + ', yet it looks VERY MUCH LIKE\none of the ' + type + 's in our database by [' + response.data.author + '] (please be warned it’s not identical)\nVerify By Alipsé');
            }else{
                msg.reply('Not an Alipsé Sealed ' + type + ', we cannot determine its authenticity\nVerify By AliPsé');
            }
            /*
            const fileExist = INPUT_PATH + filename;
            const fileVerif = OUTPUT_PATH + filename;
            const fileFake = OUTPUT_PATH +uuid + '_differeFake.jpg';
            const fileNotSealed = OUTPUT_PATH + uuid +'_notsealed.jpg';
            let fexist=false;
            let fverif=false;
            let ffake=false;
            let fnotsaled=false;

            setTimeout(()=>{
                setTimeout(()=>{
                },3000)
                if (fs.existsSync(fileExist)) {
                    fexist=true; //console.log('El archivo todavía existe...');
                } else {
                    //fexist=false;//console.log('El archivo no existe...');
                }
                if (fs.existsSync(fileVerif)) {
                    fverif=true; //console.log('El archivo todavía existe...');
                } else {
                    //fverif=false;//console.log('El archivo no existe...');
                }
                if (fs.existsSync(fileFake)) {
                    ffake=true;//console.log('El archivo todavía existe...');
                } else {
                    (fs.existsSync(fileNotSealed)) //ffake=false;//console.log('El archivo no existe...');
                }
                if (fs.existsSync(fileNotSealed)) {
                    fnotsaled=true;//console.log('El archivo todavía existe...');
                } else {
                    //fnotsaled=false;//console.log('El archivo no existe...');
                }

                console.log('Original:',fexist,' Verif:',fverif,' Fake:',ffake,' NotSealed:',fnotsaled);
                if(fverif){
                    msg.re
                    msg.reply('Alipsé Sealed TRUE image\n[the author] invests to avoid impersonation\nVerify By Alipsé');
                }
                if(ffake){
                    msg.reply('Not an Alipsé Sealed Image, yet it looks VERY MUCH LIKE\none of the images in our database by [author] (please be warned it’s not identical)\nVerify By Alipsé');
                }
                if(fnotsaled){
                    msg.reply('Not an Alipsé Sealed Image, we cannot determine its authenticity\nVerify By AliPsé');
                }
            },10000);
            */


        }else{
            console.log('Llego un archivo no procesable');
            //msg.reply('Buen día, favor de enviar una imagen o pdf a validar:');
        }
    }else{
        console.log('Llego un mensaje');
        //msg.reply('Buen día, favor de enviar una imagen o pdf a validar:');
	}
});

client.initialize();