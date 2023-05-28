package com.document.validator.documentsmicroservice.telegrambot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class TelegramBotAlipse extends TelegramLongPollingBot {

    @Override
    public String getBotUsername() {
        //Devolvemos el usuario que configuramos en BotFather
        return "AliPse_bot";
    }

    @Override
    public String getBotToken() {
        //Devolvemos el token generado por BotFather
        return "5601581111:AAERdpZHtcw5Gj7pVIX_8mdi3MHZ2CqyyAs";
    }

    @Override
    public void onUpdateReceived(Update update) {
        System.out.println("Mensaje recibido: " + update.getMessage().getText());
        final long chatId = update.getMessage().getChatId();
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Gracias por escribirnos");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}

