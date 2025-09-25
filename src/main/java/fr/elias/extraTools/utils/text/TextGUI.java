package fr.elias.extraTools.utils.text;


import fr.elias.extraTools.ExtraTools;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Consumer;

public class TextGUI<T> {

    private final ConversationFactory factory;
    private Conversation conversation;
    private final Player viewer;

    public TextGUI(Player viewer) {
        this.viewer = viewer;
        factory = new ConversationFactory(JavaPlugin.getPlugin(ExtraTools.class));
    }

    public TextGUI<T> prompt(Prompt prompt) {
        factory.withFirstPrompt(prompt);
        return this;
    }

    public TextGUI<T> timeout(int seconds) {
        factory.withTimeout(seconds);
        return this;
    }

    public TextGUI<T> exitText(String sequence) {
        factory.withEscapeSequence(sequence);
        return this;
    }

    private Conversation build() {
        if (conversation != null) {
            return conversation;
        }
        return conversation = factory.buildConversation(viewer);
    }

    @SuppressWarnings("unchecked")
    public void open(Consumer<T> consumer) {
        factory.withLocalEcho(false).addConversationAbandonedListener(event -> {
            if (event.getCanceller() != null) {
                consumer.accept(null);
            } else {
                T result = (T) event.getContext().getSessionData("result");
                consumer.accept(result);
            }
        });
        build().begin();
    }

}
