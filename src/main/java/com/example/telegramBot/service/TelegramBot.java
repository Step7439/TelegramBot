package com.example.telegramBot.service;

import com.example.telegramBot.config.BotConfig;
import com.example.telegramBot.model.Joke;
import com.example.telegramBot.model.Users;
import com.example.telegramBot.repo.RepoJoke;
import com.example.telegramBot.repo.RepoUsers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
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
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    static final String JAVA = "JAVA_BUTTON";
    static final String PYTHON = "PYTHON_BUTTON";

    static final int JOKEID = 9;
    static final String NEXT_JOKE = "NEXT";
    private final String HELP_BOT = "Настроикй бота и изменения профиля.\n" +
            "/start регистрация у бота.\n" +
            "/mydata посмотреть свой даные.\n" +
            "/settings тут можно изменить свой даные профиля.\n";

    final BotConfig botConfig;

    @Autowired
    private RepoUsers repoUsers;
    @Autowired
    private RepoJoke repoJoke;

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
                var users = repoUsers.findAll();
                for (Users user : users) {
                    sendMesseg(user.getId(), textToSend);
                }
            } else {
                switch (messagText) {
                    case "/start" -> {
                        try {
                            register(update.getMessage());
                        } catch (TelegramApiException e) {
                            log.error("Error Register user " + e.getMessage());
                        }
                        log.info("register user " + update.getMessage().getChat().getUserName());
                        startCommand(chatId, update.getMessage().getChat().getFirstName());

                    }
                    case "/mydata" -> {
                        sendMesseg(chatId, "User name : ");
                        mydataBot(update.getMessage().getChatId(), update.getMessage().getChat().getUserName());
                        sendMesseg(chatId, "First name : ");
                        mydataBot(update.getMessage().getChatId(), update.getMessage().getChat().getFirstName());
                    }
                    case "/help" -> sendMessegMenu(keyboardMac, chatId, HELP_BOT);

                    case "Погода" -> sendMessegMenu(keyboardMac, chatId, "Якутск -58");

                    case "Валюта" -> sendMessegMenu(keyboardMac, chatId, "Долар : 33.05 рублей");

                    case "Удолить профиль" -> deliteUser(update.getMessage().getChatId());

                    case "Выбор языка" -> javaPythonButton(chatId);

                    case "Анекдоты" -> {
                        try {
                            ObjectMapper objectMapper = new ObjectMapper();
                            TypeFactory typeFactory = objectMapper.getTypeFactory();
                            List<Joke> jokeList = objectMapper.readValue(new File("file/anecdotes.json"),
                                    typeFactory.constructCollectionType(List.class, Joke.class));
                            repoJoke.saveAll(jokeList);
                        } catch (Exception e) {
                            log.error("Error File" + e.getMessage());
                        }

                        var joke = getRandomJoke();
                        joke.ifPresent(valueJoke -> jokeNextButton(valueJoke.getBody(), chatId));
                    }

                    default -> sendMesseg(chatId, "Я еще маленький и не все знаю!");
                }
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            switch (callbackData) {
                case JAVA -> {
                    String str = "Ваш любимый язык Java!!!";
                    executeEditMassag(str, chatId, messageId);
                }
                case PYTHON -> {
                    String str = "Ваш любимый язык Python!!!";
                    executeEditMassag(str, chatId, messageId);
                }
                case NEXT_JOKE -> {
                    var joke = getRandomJoke();
                    joke.ifPresent(valueJoke -> jokeNextButtonWindows(valueJoke.getBody(), chatId, update.getCallbackQuery().getMessage().getMessageId()));
                }
            }
        }
    }

    private Optional<Joke> getRandomJoke() {
        var r = new Random();
        var randomId = r.nextInt(JOKEID) + 1;
        return repoJoke.findById(randomId);
    }

    private void nextButton(SendMessage message) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> listButton = new ArrayList<>();
        List<InlineKeyboardButton> buttons = new ArrayList<>();

        var jokeButton = new InlineKeyboardButton();

        jokeButton.setText(EmojiParser.parseToUnicode("Следущий анекдот" + " \uD83E\uDD23"));
        jokeButton.setCallbackData(NEXT_JOKE);

        buttons.add(jokeButton);

        listButton.add(buttons);
        markup.setKeyboard(listButton);
        message.setReplyMarkup(markup);
    }

    private void jokeNextButton(String joke, long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(joke);

        nextButton(message);
        executeMessag(message);
    }

    private void jokeNextButtonWindows(String joke, long chatId, Integer messagerId) {
        EditMessageText message = new EditMessageText();
        message.setText(joke);
        message.setChatId(chatId);
        message.setMessageId(messagerId);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> listButton = new ArrayList<>();
        List<InlineKeyboardButton> buttons = new ArrayList<>();

        var jokeButton = new InlineKeyboardButton();

        jokeButton.setText(EmojiParser.parseToUnicode("Следущий анекдот" + " \uD83E\uDD23"));
        jokeButton.setCallbackData(NEXT_JOKE);

        buttons.add(jokeButton);

        listButton.add(buttons);
        markup.setKeyboard(listButton);
        message.setReplyMarkup(markup);

        executeMessagWindows(message);
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
        if (repoUsers.findById(message.getChatId()).isEmpty()) {
            var chatId = message.getChatId();
            var chat = message.getChat();
            Users users = new Users();
            users.setId(chatId);
            users.setUsername(chat.getUserName());
            users.setTimestamp(new Timestamp(System.currentTimeMillis()));
            repoUsers.save(users);
        }
    }

    private void mydataBot(Long id, String user) {
        repoUsers.findById(id);
        sendMesseg(id, user);
    }

    private void deliteUser(Long id) {
        repoUsers.deleteById(id);
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

        row.add("Анекдоты");
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

    private void executeMessagWindows(EditMessageText message) {
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
