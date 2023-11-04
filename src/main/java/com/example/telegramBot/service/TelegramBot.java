package com.example.telegramBot.service;

import com.example.telegramBot.config.BotConfig;
import com.example.telegramBot.model.Users;
import com.example.telegramBot.repo.RepozitoryTelegramBot;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import com.example.telegramBot.config.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    static final String JAVA = "JAVA_BUTTON";
    static final String PYTHON = "PYTHON_BUTTON";
    private final String HELP_BOT = "Настроикй бота и изменения профиля.\n" +
            "/start регистрация у бота.\n" +
            "/mydata посмотреть свой даные.\n" +
            "/settings тут можно изменить свой даные профиля.\n";

    final BotConfig botConfig;

    @Autowired
    private RepozitoryTelegramBot repozitoryTelegramBot;

    public TelegramBot(BotConfig botConfig) {
        this.botConfig = botConfig;
        List<BotCommand> botCommandList = new ArrayList<>();
        botCommandList.add(new BotCommand("/start", "Приветствие!!"));
        botCommandList.add(new BotCommand("/mydata", "Мой даные "));
        botCommandList.add(new BotCommand("/help", "Как использовать бота"));
        botCommandList.add(new BotCommand("/settings", "Изменения даных"));

        try {
            this.execute(new SetMyCommands(botCommandList, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error BotComand" + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return botConfig.getName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messagText = update.getMessage().getText();
            ReplyKeyboardMarkup keyboardMac = new ReplyKeyboardMarkup();
            long chatId = update.getMessage().getChatId();

            if (messagText.contains("/send") && botConfig.getAdminId() == chatId) {
                var textToSend = EmojiParser.parseToUnicode(messagText.substring(messagText.indexOf(" ")));
                var users = repozitoryTelegramBot.findAll();
                for (Users user : users) {
                    sendMesseg(user.getId(), textToSend);
                }
            } else {
                switch (messagText) {
                    case "/start":
                        try {
                            register(update.getMessage());
                        } catch (TelegramApiException e) {
                            log.error("Error Register user " + e.getMessage());
                        }
                        log.info("register user " + update.getMessage().getChat().getUserName());
                        startCommand(chatId, update.getMessage().getChat().getFirstName());
                        break;
                    case "/mydata":
                        sendMesseg(chatId, "User name : ");
                        mydataBot(update.getMessage().getChatId(), update.getMessage().getChat().getUserName());
                        sendMesseg(chatId, "First name : ");
                        mydataBot(update.getMessage().getChatId(), update.getMessage().getChat().getFirstName());
                        break;
                    case "/help":
                        sendMessegMenu(keyboardMac, chatId, HELP_BOT);
                        break;
                    case "Погода":
                        sendMessegMenu(keyboardMac, chatId, "Якутск -58");
                        break;
                    case "Валюта":
                        sendMessegMenu(keyboardMac, chatId, "Долар : 33.05 рублей");
                        break;
                    case "Удолить профиль":
                        deliteUser(update.getMessage().getChatId());
                        break;
                    case "Выбор языка":
                        javaPythonButton(chatId);
                        break;
                    default:
                        sendMesseg(chatId, "Я еще маленький и не все знаю!");
                }
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callbackData.equals(JAVA)) {
                String str = "Ваш любимый язык Java!!!";
                executeEditMassag(str, chatId, messageId);
            } else if (callbackData.equals(PYTHON)) {
                String str = "Ваш любимый язык Python!!!";
                executeEditMassag(str, chatId, messageId);
            }

        }

    }

    private void javaPythonButton(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Какой ваш любимый язык ?");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> listButton = new ArrayList<>();
        List<InlineKeyboardButton> buttons = new ArrayList<>();

        var yesButton = new InlineKeyboardButton();

        yesButton.setText("Java");
        yesButton.setCallbackData(JAVA);

        var noButton = new InlineKeyboardButton();

        noButton.setText("Python");
        noButton.setCallbackData(PYTHON);

        buttons.add(yesButton);
        buttons.add(noButton);

        listButton.add(buttons);
        markup.setKeyboard(listButton);
        message.setReplyMarkup(markup);

        executeMessag(message);

    }

    private void register(Message message) throws TelegramApiException {
        if (repozitoryTelegramBot.findById(message.getChatId()).isEmpty()) {
            var chatId = message.getChatId();
            var chat = message.getChat();
            Users users = new Users();
            users.setId(chatId);
            users.setUsername(chat.getUserName());
            users.setTimestamp(new Timestamp(System.currentTimeMillis()));
            repozitoryTelegramBot.save(users);
        }
    }

    private void mydataBot(Long id, String user) {
        repozitoryTelegramBot.findById(id);
        sendMesseg(id, user);
    }

    private void deliteUser(Long id) {
        repozitoryTelegramBot.deleteById(id);
    }

    private void startCommand(long chatId, String name) {
        String answer = EmojiParser.parseToUnicode("Привет " + name + ", молодец что ты с нами " + "\uD83D\uDE0A" + ":blush: ");
        sendMesseg(chatId, answer);
    }

    private void sendMesseg(long chatId, String text) {

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        executeMessag(message);
    }

    private void sendMessegMenu(ReplyKeyboardMarkup keyboardMarkup, long chatId, String text) {

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        keyboardMarkup.setKeyboard(keyboard());
        message.setReplyMarkup(keyboardMarkup);

        executeMessag(message);
    }

    private List<KeyboardRow> keyboard() {
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        row.add("Погода");
        row.add("Валюта");
        keyboardRows.add(row);
        row = new KeyboardRow();
        row.add("Выбор языка");
        row.add("Удолить профиль");
        keyboardRows.add(row);

        return keyboardRows;
    }

    private void executeMessag(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error :" + e.getMessage());
        }
    }

    private void executeEditMassag(String str, long chatId, long messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setText(str);
        message.setMessageId((int) messageId);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error :" + e.getMessage());
        }
    }
}
