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
    private double carPrice = 0; // The car price entered by the user
    private Map<String, Double> interestRates = new HashMap<>();
    private Map<String, Integer> loanTerms = new HashMap<>();

    public TelegramBot() {
        // Interest rates from the table
        // 30% downpayment
        interestRates.put("prepay_30_term_12", 3.49);
        interestRates.put("prepay_30_term_24", 6.99);
        interestRates.put("prepay_30_term_36", 8.99);
        interestRates.put("prepay_30_term_48", 11.99);
        interestRates.put("prepay_30_term_60", 11.99);

        // 40% downpayment
        interestRates.put("prepay_40_term_12", 2.49);
        interestRates.put("prepay_40_term_24", 5.99);
        interestRates.put("prepay_40_term_36", 8.49);
        interestRates.put("prepay_40_term_48", 11.99);
        interestRates.put("prepay_40_term_60", 11.99);

        // 50% downpayment
        interestRates.put("prepay_50_term_12", 1.49);
        interestRates.put("prepay_50_term_24", 4.99);
        interestRates.put("prepay_50_term_36", 7.49);
        interestRates.put("prepay_50_term_48", 9.99);
        interestRates.put("prepay_50_term_60", 9.99);

        // 60% downpayment
        interestRates.put("prepay_60_term_12", 0.01);
        interestRates.put("prepay_60_term_24", 3.99);
        interestRates.put("prepay_60_term_36", 5.49);
        interestRates.put("prepay_60_term_48", 9.99);
        interestRates.put("prepay_60_term_60", 9.99);

        // 70% downpayment
        interestRates.put("prepay_70_term_12", 0.01);
        interestRates.put("prepay_70_term_24", 0.01);
        interestRates.put("prepay_70_term_36", 4.99);
        interestRates.put("prepay_70_term_48", 7.99);
        interestRates.put("prepay_70_term_60", 7.99);

        // Loan terms in months
        loanTerms.put("prepay_30_term_12", 12);
        loanTerms.put("prepay_30_term_24", 24);
        loanTerms.put("prepay_30_term_36", 36);
        loanTerms.put("prepay_30_term_48", 48);
        loanTerms.put("prepay_30_term_60", 60);

        loanTerms.put("prepay_40_term_12", 12);
        loanTerms.put("prepay_40_term_24", 24);
        loanTerms.put("prepay_40_term_36", 36);
        loanTerms.put("prepay_40_term_48", 48);
        loanTerms.put("prepay_40_term_60", 60);

        loanTerms.put("prepay_50_term_12", 12);
        loanTerms.put("prepay_50_term_24", 24);
        loanTerms.put("prepay_50_term_36", 36);
        loanTerms.put("prepay_50_term_48", 48);
        loanTerms.put("prepay_50_term_60", 60);

        loanTerms.put("prepay_60_term_12", 12);
        loanTerms.put("prepay_60_term_24", 24);
        loanTerms.put("prepay_60_term_36", 36);
        loanTerms.put("prepay_60_term_48", 48);
        loanTerms.put("prepay_60_term_60", 60);

        loanTerms.put("prepay_70_term_12", 12);
        loanTerms.put("prepay_70_term_24", 24);
        loanTerms.put("prepay_70_term_36", 36);
        loanTerms.put("prepay_70_term_48", 48);
        loanTerms.put("prepay_70_term_60", 60);

    }

    @Override
    public String getBotUsername() {
        return "Art_car_calculator_bot"; // Replace with your bot's username
    }

    @Override
    public String getBotToken() {
        return "7524858228:AAHOOHHDHn_DJECA_jvEahBWS1VoCI70Yro"; // Replace with your bot's token
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.equals("/start")) {
                sendMessage(chatId, "Ласкаво просимо! Будь ласка, введіть ціну автомобіля.");
            } else if (messageText.matches("\\d+")) {
                carPrice = Double.parseDouble(messageText);
                sendMessage(chatId, "You entered a car price of " + carPrice + " UAH.");
                sendOptions(chatId); // Show buttons after price is entered
            } else {
                sendMessage(chatId, "Please enter a valid car price in numbers.");
            }
        } else if (update.hasCallbackQuery()) {


            if (update.hasCallbackQuery()) {
                String callbackData = update.getCallbackQuery().getData();
                long chatId = update.getCallbackQuery().getMessage().getChatId();

                // Ensure callback data is valid
                if (interestRates.containsKey(callbackData)) {
                    double interestRate = interestRates.get(callbackData); // Процентная ставка в процентах
                    int loanTerm = loanTerms.get(callbackData); // Период кредита в месяцах
                    double prepaymentPercentage = getPrepaymentPercentage(callbackData); // Процент первоначального взноса

                    // Предполагаем, что carPrice (цена автомобиля) уже введена пользователем и установлена
                    double prepaymentAmount = carPrice * prepaymentPercentage;  // Первоначальный взнос
                    double loanAmount = carPrice - prepaymentAmount;  // Сумма кредита

                    // Полная сумма кредита с процентами
                    double totalLoanWithInterest = loanAmount * (1 + interestRate / 100);
                    double totalAmountPaid = prepaymentAmount + totalLoanWithInterest;  // Полная сумма к оплате

                    // Ежемесячный платеж
                    double monthlyPayment = totalLoanWithInterest / loanTerm;

                    // Переплата
                    double overpayment = totalAmountPaid - carPrice;

                    // Формирование ответа на украинском языке
                    String result = String.format(
                            "Для передоплати %.0f%% та терміну %d місяців із річною ставкою %.2f%%:\n" +
                                    "Передоплата: %.2f UAH\n" +
                                    "Сума кредиту: %.2f UAH\n" +
                                    "Загалом до сплати: %.2f UAH\n" +
                                    "Переплата: %.2f UAH\n" +
                                    "**Щомісячний платіж: %.2f UAH**",
                            prepaymentPercentage * 100, loanTerm, interestRate, prepaymentAmount, totalLoanWithInterest, totalAmountPaid, overpayment, monthlyPayment
                    );
                    sendMessage(chatId, result);
                }
            }
        }
    }

    // Method to extract prepayment percentage
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

    // Method to send a message
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
        message.setText("Choose your prepayment and loan period:");
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }

    private List<InlineKeyboardButton> createRow(String text, String callbackData) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        row.add(button);
        return row;
    }


}
