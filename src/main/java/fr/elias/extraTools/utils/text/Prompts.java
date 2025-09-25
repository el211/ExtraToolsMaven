package fr.elias.extraTools.utils.text;


import fr.elias.extraTools.utils.config.Msgs;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.ValidatingPrompt;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public record Prompts<T>(String errorMsg, Predicate<String> predicate, Function<String, T> converter) {

    public static final Prompts<String> TEXT = new Prompts<>(null, s -> true, Objects::toString);
    public static final Prompts<PotionEffectType> EFFECT = new Prompts<>("%s% is not a valid potion", s -> PotionEffectType.getByName(s.toLowerCase()) != null, PotionEffectType::getByName);
    public static final Prompts<Number> NUM = new Prompts<>("%s% is not a valid number", NumberUtils::isCreatable, NumberUtils::createNumber);
    public static final Prompts<Boolean> BOOL = new Prompts<>("%s% is an invalid boolean", s -> BooleanUtils.toBooleanObject(s) != null, BooleanUtils::toBoolean);

    public ValidatingPrompt asPrompt(String displayText) {
        return new ValidatingPrompt() {

            @Override
            public String getPromptText(ConversationContext conversationContext) {
                String lineOne = Msgs.of(displayText).asLegacy();
                String lineTwo = Msgs.of("<gray>(Write 'cancel' or wait one minute to cancel)").asLegacy();
                return lineOne + "\n" + lineTwo;
            }

            @Override
            protected String getFailedValidationText(ConversationContext context, String invalidInput) {
                return Msgs.of(errorMsg).var("s", invalidInput).asLegacy();
            }

            @Override
            protected boolean isInputValid(ConversationContext conversationContext, String string) {
                return predicate.test(string);
            }

            @Override
            protected Prompt acceptValidatedInput(ConversationContext conversationContext, String string) {
                conversationContext.setSessionData("result", converter.apply(string));
                return END_OF_CONVERSATION;
            }
        };
    }

    public TextGUI<T> asConv(Player who, String displayText) {
        return new TextGUI<T>(who).prompt(asPrompt(displayText));
    }

    public void start(Player who, String displayText, Consumer<T> consumer) {
        asConv(who, displayText).exitText("cancel").timeout(60).open(consumer);
    }

}
