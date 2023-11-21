const { Client, LocalAuth } = require('whatsapp-web.js');
const qrcode = require('qrcode-terminal');
const readable = require('stream').Readable;
const fs = require("fs");
const { v4: uuidv4 } = require('uuid');
const INPUT_PATH = 'C:/app/alipse/telegramIn/';
const OUTPUT_PATH = 'C:/app/alipse/telegramOut/';

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
    if(msg.hasMedia) {
        const media = await msg.downloadMedia();
        if(media.mimetype.includes("image")){
            console.log(media.filename);
            console.log(media.filesize);
            console.log(media.mimetype);
            
            // do something with the media data here
            let uuid = uuidv4();
            let filename = uuid + "." + media.mimetype.split("/")[1];

            const fileBuffer = Buffer.from(media.data,'base64');
            const stream = new readable();
            stream.push(fileBuffer);
            stream.push(null);
            stream.pipe(fs.createWriteStream(INPUT_PATH + filename)); // Here I know the file type so added .pdf as extension.

            client.sendMessage(msg.from, 'You have just uploaded a static image, its authenticity will be validated, please wait while it finishes...');

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
        }else{
            console.log('Llego un archivo no procesable');
        }
    }else{
        console.log('Llego un mensaje');
        //msg.reply('Buen día, favor de enviar el archivo a validar:');
	}
});

client.initialize();