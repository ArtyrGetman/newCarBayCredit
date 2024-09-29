package org.example;


import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TelegramBot extends TelegramLongPollingBot {
    private double carPrice = 0; // Цена автомобиля, введённая пользователем
    private String selectedSalon = ""; // Выбранный автосалон
    private String selectedBank = "";  // Выбранный банк

    // Хранение кредитных условий для банков
    private Map<String, Map<String, Double>> interestRates = new HashMap<>();
    private Map<String, Map<String, Integer>> loanTerms = new HashMap<>();

    // Автосалоны и доступные банки
    private Map<String, List<String>> salonsAndBanks = new HashMap<>();

    public TelegramBot() {
        // Инициализация автосалонов и банков
        List<String> mazdaBanks = new ArrayList<>();
        mazdaBanks.add("PrivatBank");
        mazdaBanks.add("OschadBank");
        salonsAndBanks.put("MAZDA", mazdaBanks);

        List<String> renoBanks = new ArrayList<>();
        renoBanks.add("ABank");
        salonsAndBanks.put("RENO", renoBanks);

        List<String> toyotaBanks = new ArrayList<>();
        toyotaBanks.add("PrivatBank");
        toyotaBanks.add("Lising");
        salonsAndBanks.put("TOYOTA", toyotaBanks);

        // Инициализация процентных ставок и условий для каждого банка
        // Пример для PrivatBank (для каждого салона и банка можно создать свою кредитную сетку)
        Map<String, Double> privatRates = new HashMap<>();
        privatRates.put("prepay_30_term_12", 3.49);
        privatRates.put("prepay_30_term_24", 6.99);
        interestRates.put("PrivatBank", privatRates);

        Map<String, Integer> privatTerms = new HashMap<>();
        privatTerms.put("prepay_30_term_12", 12);
        privatTerms.put("prepay_30_term_24", 24);
        loanTerms.put("PrivatBank", privatTerms);

        // Пример для OschadBank
        Map<String, Double> oschadRates = new HashMap<>();
        oschadRates.put("prepay_40_term_12", 2.49);
        oschadRates.put("prepay_40_term_24", 5.99);
        interestRates.put("OschadBank", oschadRates);

        Map<String, Integer> oschadTerms = new HashMap<>();
        oschadTerms.put("prepay_40_term_12", 12);
        oschadTerms.put("prepay_40_term_24", 24);
        loanTerms.put("OschadBank", oschadTerms);
    }

    @Override
    public String getBotUsername() {
        return "Art_car_calculator_bot"; // Замените на имя вашего бота
    }

    @Override
    public String getBotToken() {
        return "7524858228:AAHOOHHDHn_DJECA_jvEahBWS1VoCI70Yro"; // Замените на токен вашего бота
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            // Если пользователь отправил команду /start
            if (messageText.equals("/start")) {
                sendMessage(chatId, "Добро пожаловать! Пожалуйста, выберите автосалон:");
                sendSalonOptions(chatId); // Отправляем кнопки с автосалонами

                // Если пользователь вводит цену автомобиля, но при этом салон и банк уже выбраны
            } else if (messageText.matches("\\d+") && !selectedSalon.isEmpty() && !selectedBank.isEmpty()) {
                carPrice = Double.parseDouble(messageText);
                sendMessage(chatId, "Вы ввели цену автомобиля " + carPrice + " UAH.");
                sendLoanOptions(chatId); // Показать кнопки с кредитными условиями

                // Если цена введена до выбора салона или банка
            } else if (messageText.matches("\\d+") && (selectedSalon.isEmpty() || selectedBank.isEmpty())) {
                sendMessage(chatId, "Сначала выберите автосалон и банк.");
            } else {
                sendMessage(chatId, "Введите корректную цену автомобиля.");
            }

        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            // Пользователь выбрал автосалон
            if (callbackData.startsWith("salon_")) {
                selectedSalon = callbackData.replace("salon_", "").toUpperCase();
                sendMessage(chatId, "Вы выбрали салон: " + selectedSalon + ". Теперь выберите банк:");
                sendBankOptions(chatId); // Отправляем список банков для выбранного салона

                // Пользователь выбрал банк
            } else if (callbackData.startsWith("bank_")) {
                selectedBank = callbackData.replace("bank_", "");
                sendMessage(chatId, "Вы выбрали банк: " + selectedBank + ". Теперь введите цену автомобиля.");

                // Пользователь выбрал кредитные условия
            } else if (interestRates.get(selectedBank) != null && interestRates.get(selectedBank).containsKey(callbackData)) {
                handleLoanCalculation(chatId, callbackData); // Обработка расчета кредита
            }
        }
    }

    // Метод для отправки кнопок с автосалонами
    private void sendSalonOptions(long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Добавляем кнопки с названиями автосалонов
        for (String salon : salonsAndBanks.keySet()) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(createButton(salon, "salon_" + salon.toLowerCase()));
            rows.add(row);
        }

        markup.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите автосалон:");
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Метод для отправки кнопок с банками
    private void sendBankOptions(long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Получаем список банков для выбранного автосалона
        List<String> banks = salonsAndBanks.get(selectedSalon);
        if (banks != null && !banks.isEmpty()) {
            for (String bank : banks) {
                List<InlineKeyboardButton> row = new ArrayList<>();
                row.add(createButton(bank, "bank_" + bank));
                rows.add(row);
            }
            markup.setKeyboard(rows);

            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText("Выберите банк:");
            message.setReplyMarkup(markup);

            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            // Если у выбранного салона нет доступных банков
            sendMessage(chatId, "К сожалению, для выбранного салона нет доступных банков.");
            sendSalonOptions(chatId); // Возвращаемся к выбору салона
        }
    }



    // Метод для отображения кнопок с кредитными условиями (процентными ставками и сроками)
    private void sendLoanOptions(long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Получаем ставки и условия для выбранного банка
        Map<String, Double> rates = interestRates.get(selectedBank);
        Map<String, Integer> terms = loanTerms.get(selectedBank);

        if (rates != null && terms != null) {
            for (String key : rates.keySet()) {
                List<InlineKeyboardButton> row = new ArrayList<>();
                String buttonText = String.format("%d месяцев / %.2f%%", terms.get(key), rates.get(key));
                row.add(createButton(buttonText, key));
                rows.add(row);
            }
        }

        markup.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите условия кредита:");
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Метод для обработки расчета кредита
    private void handleLoanCalculation(long chatId, String callbackData) {
        try {
            Map<String, Double> bankRates = interestRates.get(selectedBank);
            Map<String, Integer> bankTerms = loanTerms.get(selectedBank);

            if (bankRates != null && bankTerms != null) {
                double interestRate = bankRates.get(callbackData); // Процентная ставка
                int loanTerm = bankTerms.get(callbackData); // Период кредита в месяцах
                double prepaymentPercentage = getPrepaymentPercentage(callbackData); // Процент первоначального взноса

                double prepaymentAmount = carPrice * prepaymentPercentage;  // Первоначальный взнос
                double loanAmount = carPrice - prepaymentAmount;  // Сумма кредита

                // Полная сумма кредита с процентами
                double totalLoanWithInterest = loanAmount * (1 + interestRate / 100);
                double totalAmountPaid = prepaymentAmount + totalLoanWithInterest;  // Полная сумма к оплате

                // Ежемесячный платёж
                double monthlyPayment = totalLoanWithInterest / loanTerm;

                // Переплата
                double overpayment = totalAmountPaid - carPrice;

                // Формирование ответа
                String result = String.format(
                        "Для автосалона %s, банка %s, передоплаты %.0f%% и срока %d месяцев с процентной ставкой %.2f%%:\n" +
                                "Передоплата: %.2f UAH\n" +
                                "Сумма кредита: %.2f UAH\n" +
                                "Всего к оплате: %.2f UAH\n" +
                                "Переплата: %.2f UAH\n" +
                                "**Ежемесячный платёж: %.2f UAH**",
                        selectedSalon, selectedBank, prepaymentPercentage * 100, loanTerm, interestRate, prepaymentAmount,
                        totalLoanWithInterest, totalAmountPaid, overpayment, monthlyPayment
                );
                sendMessage(chatId, result);
            }
        } catch (Exception e) {
            sendMessage(chatId, "Произошла ошибка при расчете. Пожалуйста, попробуйте снова.");
        }
    }

    // Метод для извлечения процента первоначального взноса
    private double getPrepaymentPercentage(String callbackData) {
        if (callbackData.contains("prepay_30")) {
            return 0.30;
        } else if (callbackData.contains("prepay_40")) {
            return 0.40;
        } else if (callbackData.contains("prepay_50")) {
            return 0.50;
        } else if (callbackData.contains("prepay_60")) {
            return 0.60;
        } else if (callbackData.contains("prepay_70")) {
            return 0.70;
        }
        return 0;
    }

    // Метод для отправки сообщений
    private void sendMessage(long chatId, String text) {
        var message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Метод для создания кнопок
    private InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }


}
