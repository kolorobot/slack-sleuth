package pl.codeleak.slack.sleuth;

import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.model.*;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.*;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Command(name = "channels")
class ConversationsList implements Callable<Integer> {

    public static final int DEFAULT_LIMIT = 2;

    @ParentCommand
    private App app;

    @Option(names = {"-s", "--search"})
    private Optional<String> searchTerm;

    @Override
    public Integer call() {
        return fetchConversations(app.slackToken);
    }

    int fetchConversations(String token) {
        var client = Slack.getInstance().methods();
        var channels = new ArrayList<Conversation>();
        var hasMoreResults = false;
        var nextCursor = new AtomicReference<>("");
        do {
            try {
                var result = client.conversationsList(r -> r
                        .token(token)
                        .limit(DEFAULT_LIMIT)
                        .excludeArchived(true)
                        .types(List.of(ConversationType.PUBLIC_CHANNEL))
                        .cursor(nextCursor.get())
                );

                if (!result.isOk()) {
                    log.error("Slack Web API failure. Error: '{}'", result.getError());
                    return -1;
                }

                channels.addAll(Optional.ofNullable(result.getChannels()).orElse(Collections.emptyList()));

                if (!result.getResponseMetadata().getNextCursor().isBlank()) {
                    hasMoreResults = true;
                    nextCursor.set(result.getResponseMetadata().getNextCursor());
                } else {
                    hasMoreResults = false;
                    nextCursor.set("");
                }

            } catch (IOException | SlackApiException e) {
                log.error("error while fetching channels: {}", e.getMessage(), e);
                return -1;
            }
        } while ((hasMoreResults));

        print(channels);

        return 0;
    }

    private void print(List<Conversation> conversations) {
        conversations.stream()
                .filter(conversation -> searchTerm.map(val -> conversation.getName().matches(".*" + val + ".*")).orElse(true))
                .peek(conversation -> log.info(conversation.getName() + " (id: " + conversation.getId() + ")"))
                .toList();
    }


}
