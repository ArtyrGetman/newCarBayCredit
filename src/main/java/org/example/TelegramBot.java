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
    private Map<String, Double> interestRates = new HashMap<>();
    private Map<String, Integer> loanTerms = new HashMap<>();

    public TelegramBot() {
        // Процентные ставки и сроки кредита инициализируются как в вашем примере
        // (оставим их без изменений для простоты)
    }

    @Override
    public String getBotUsername() {
        return "Art_car_calculator_bot"; // Замените на имя вашего бота
    }

    @Override
    public String getBotToken() {
        return "ТОКЕН_ВАШЕГО_БОТА"; // Замените на токен вашего бота
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.equals("/start")) {
                sendMessage(chatId, "Добро пожаловать! Пожалуйста, выберите автосалон:");
                sendSalonOptions(chatId); // Отправляем кнопки с автосалонами
            } else if (messageText.matches("\\d+")) {
                carPrice = Double.parseDouble(messageText);
                sendMessage(chatId, "Вы ввели цену автомобиля " + carPrice + " UAH.");
                sendOptions(chatId); // Показать кнопки после ввода цены
            } else {
                sendMessage(chatId, "Введите, пожалуйста, корректную цену автомобиля.");
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callbackData.startsWith("salon_")) {
                // Пользователь выбрал автосалон
                selectedSalon = callbackData.replace("salon_", "");
                sendMessage(chatId, "Вы выбрали салон: " + selectedSalon + ". Теперь введите цену автомобиля.");
            } else if (interestRates.containsKey(callbackData)) {
                // Обработка выбора кредитных условий после ввода цены автомобиля
                handleLoanCalculation(chatId, callbackData);
            }
        }
    }

    // Метод для отправки кнопок с автосалонами
    private void sendSalonOptions(long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Добавляем кнопки с названиями автосалонов
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createButton("MAZDA", "salon_mazda"));
        row1.add(createButton("RENO", "salon_reno"));
        row1.add(createButton("TOYOTA", "salon_toyota"));
        rows.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createButton("PORSHE", "salon_porshe"));
        row2.add(createButton("HONDA", "salon_honda"));
        row2.add(createButton("HYUNDAI", "salon_hyundai"));
        rows.add(row2);

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

    // Метод для обработки расчета кредита
    private void handleLoanCalculation(long chatId, String callbackData) {
        try {
            double interestRate = interestRates.get(callbackData); // Процентная ставка
            int loanTerm = loanTerms.get(callbackData); // Период кредита в месяцах
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

            // Формирование ответа на украинском языке
            String result = String.format(
                    "Для автосалона %s, передоплати %.0f%% та терміну %d місяців із річною ставкою %.2f%%:\n" +
                            "Передоплата: %.2f UAH\n" +
                            "Сума кредиту: %.2f UAH\n" +
                            "Загалом до сплати: %.2f UAH\n" +
                            "Переплата: %.2f UAH\n" +
                            "**Щомісячний платіж: %.2f UAH**",
                    selectedSalon, prepaymentPercentage * 100, loanTerm, interestRate, prepaymentAmount,
                    totalLoanWithInterest, totalAmountPaid, overpayment, monthlyPayment
            );
            sendMessage(chatId, result);
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

    // Метод для отправки сообщения
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


    private void sendOptions(long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Example for 30% prepayment (one row with all terms for 30% prepayment)
        List<InlineKeyboardButton> row30 = new ArrayList<>();
        row30.add(createButton("30% - 12m/3.49%", "prepay_30_term_12"));
        row30.add(createButton("30% - 24m/6.99%", "prepay_30_term_24"));
        row30.add(createButton("30% - 36m/8.99%", "prepay_30_term_36"));
        row30.add(createButton("30% - 48m/11.99%", "prepay_30_term_48"));
        row30.add(createButton("30% - 60m/11.99%", "prepay_30_term_60"));
        rows.add(row30); // Add row to rows list

        // Example for 40% prepayment (one row with all terms for 40% prepayment)
        List<InlineKeyboardButton> row40 = new ArrayList<>();
        row40.add(createButton("40% - 12m/2.49%", "prepay_40_term_12"));
        row40.add(createButton("40% - 24m/5.99%", "prepay_40_term_24"));
        row40.add(createButton("40% - 36m/8.49%", "prepay_40_term_36"));
        row40.add(createButton("40% - 48m/11.99%", "prepay_40_term_48"));
        row40.add(createButton("40% - 60m/11.99%", "prepay_40_term_60"));
        rows.add(row40); // Add row to rows list

        // Example for 50% prepayment (one row with all terms for 50% prepayment)
        List<InlineKeyboardButton> row50 = new ArrayList<>();
        row50.add(createButton("50% - 12m/1.49%", "prepay_50_term_12"));
        row50.add(createButton("50% - 24m/4.99%", "prepay_50_term_24"));
        row50.add(createButton("50% - 36m/7.49%", "prepay_50_term_36"));
        row50.add(createButton("50% - 48m/9.99%", "prepay_50_term_48"));
        row50.add(createButton("50% - 60m/9.99%", "prepay_50_term_60"));
        rows.add(row50); // Add row to rows list

        // Example for 60% prepayment (one row with all terms for 60% prepayment)
        List<InlineKeyboardButton> row60 = new ArrayList<>();
        row60.add(createButton("60% - 12m/0.01%", "prepay_60_term_12"));
        row60.add(createButton("60% - 24m/3.99%", "prepay_60_term_24"));
        row60.add(createButton("60% - 36m/5.49%", "prepay_60_term_36"));
        row60.add(createButton("60% - 48m/9.99%", "prepay_60_term_48"));
        row60.add(createButton("60% - 60m/9.99%", "prepay_60_term_60"));
        rows.add(row60); // Add row to rows list

        // Example for 70% prepayment (one row with all terms for 70% prepayment)
        List<InlineKeyboardButton> row70 = new ArrayList<>();
        row70.add(createButton("70% - 12m/0.01%", "prepay_70_term_12"));
        row70.add(createButton("70% - 24m/0.01%", "prepay_70_term_24"));
        row70.add(createButton("70% - 36m/4.99%", "prepay_70_term_36"));
        row70.add(createButton("70% - 48m/7.99%", "prepay_70_term_48"));
        row70.add(createButton("70% - 60m/7.99%", "prepay_70_term_60"));
        rows.add(row70); // Add row to rows list

        markup.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Виберіть передоплату і термін кредиту:");
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Helper method to create buttons



}
