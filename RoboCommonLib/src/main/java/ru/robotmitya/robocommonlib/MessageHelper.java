package ru.robotmitya.robocommonlib;

/**
 * Created by dmitrydzz on 1/28/14.
 * Набор методов для сборки и разбора с сообщений.
 * @author Дмитрий Дзахов
 *
 */
public final class MessageHelper {
    /**
     * Длина сообщений, между уровнями ПК, телефон, контроллер.
     */
    public static final int MESSAGE_LENGTH = 5;

    /**
     * Длина идентификатора в сообщении.
     */
    public static final int MESSAGE_IDENTIFIER_LENGTH = 1;

    /**
     * Длина значения в сообщении.
     */
    public static final int MESSAGE_VALUE_LENGTH = 4;

    /**
     * Конструктор спрятан.
     */
    private MessageHelper() {
    }

    /**
     * Символ, которым дополняется идентификатор сообщения если ему не хватает
     * длины до SettingsActivity.MESSAGE_IDENTIFIER_LENGTH.
     */
    private static final char IDENTIFIER_PREFIX = ' ';

    /**
     * Символ, которым дополняется значение из сообщения если ему не хватает длины до SettingsActivity.MESSAGE_VALUE_LENGTH.
     */
    private static final char VALUE_PREFIX = '0';

    /**
     * Дополняет заданный текст справа указанными символами до указанной длины.
     * Если длина превышает указанный размер, текст обрезается.
     * @param text исходный текст.
     * @param length требуемая длина.
     * @param prefixChar символ, добавляемый справа.
     * @return скорректированный текст.
     */
    public static String correctLength(final String text, final int length, final char prefixChar) {
        String result = text;
        int sourceLength = text.length();
        if (sourceLength > length) {
            result = text.substring(sourceLength - length, sourceLength);
//            result = text.substring(0, length);
        } else if (sourceLength < length) {
            int charsToAdd = length - sourceLength;
            for (int i = 0; i < charsToAdd; i++) {
                result = prefixChar + result;
            }
        }

        return result;
    }

    /**
     * Сборка сообщения по идентификатору и значению.
     * @param messageIdentifier идентификатор сообщения.
     * @param messageValue значение сообщения.
     * @return сообщение.
     */
    public static String makeMessage(final String messageIdentifier, final String messageValue) {
        String identifier = correctLength(messageIdentifier, MESSAGE_IDENTIFIER_LENGTH, IDENTIFIER_PREFIX);
        String value = correctLength(messageValue, MESSAGE_VALUE_LENGTH, VALUE_PREFIX);
        return identifier.concat(value);
    }

    /**
     * Building message from identifier and integer value.
     * @param messageIdentifier message identifier chars.
     * @param messageValue signed 2 byte integer value.
     * @return text message.
     */
    public static String makeMessage(final String messageIdentifier, final short messageValue) {
        String identifier = correctLength(messageIdentifier, MESSAGE_IDENTIFIER_LENGTH, IDENTIFIER_PREFIX);
        String hexValue = Integer.toHexString(messageValue).toUpperCase();
        hexValue = MessageHelper.correctLength(hexValue, MESSAGE_VALUE_LENGTH, VALUE_PREFIX);
        return identifier.concat(hexValue);
    }

    /**
     * Извлечение идентификатора из сообщения.
     * @param message сообщение.
     * @return идентификатор сообщения.
     */
    public static String getMessageIdentifier(final String message) {
        int realLength = Math.min(message.length(), MESSAGE_IDENTIFIER_LENGTH);
        String result = message.substring(0, realLength);
        result = correctLength(result, MESSAGE_IDENTIFIER_LENGTH, IDENTIFIER_PREFIX);
        return result;
    }

    /**
     * Извлечение значения из сообщения.
     * @param message сообщение.
     * @return значение из сообщения.
     */
    public static String getMessageStringValue(final String message) {
        String result;

        // Первые символы считаем идентификатором.
        if (message.length() <= MESSAGE_IDENTIFIER_LENGTH) {
            result = "";
        } else {
            result = message.substring(MESSAGE_IDENTIFIER_LENGTH, message.length());
        }

        result = correctLength(result, MESSAGE_VALUE_LENGTH, VALUE_PREFIX);
        return result;
    }

    public static int getMessageIntegerValue(final String message) {
        String textValue = MessageHelper.getMessageStringValue(message);
        if (textValue.isEmpty()) {
            return 0;
        }

        return Integer.valueOf(textValue, 16);
    }

    /**
     * Input stream can have broken messages. Some bytes could be lost. So the goal is to find and execute first message.
     * Theoretically it should start with 0 position. But if first message is broken
     * it can start with a hex digit (not command char). This function returns message stream without this broken prefix.
     * @param messages - input message stream.
     * @return input message stream without first broken message (if exists).
     */
    public static String skipFirstBrokenMessage(final String messages) {
        int firstMessagePosition = getFirstMessagePosition(messages);
        if (firstMessagePosition < 0) {
            return "";
        }
        return messages.substring(firstMessagePosition);
    }

    /**
     * Finds the position of the first message in input message stream.
     * @param messages - input message stream.
     * @return the position of first message in input stream or -1 if it is not found.
     */
    public static int getFirstMessagePosition(final String messages) {
        int result = -1;
        for (int i = 0; i < messages.length(); i++) {
            char c = messages.charAt(i);
            if (((c >= '0') && (c <= '9'))
                    || ((c >= 'A') && (c <= 'F'))
                    || ((c >= 'a') && (c <= 'f'))) {
                continue;
            }
            result = i;
            break;
        }
        return result;
    }
}