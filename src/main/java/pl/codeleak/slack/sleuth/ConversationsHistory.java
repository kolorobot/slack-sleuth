package pl.codeleak.slack.sleuth;

import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.model.Message;
import com.slack.api.util.json.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.emptyList;

@Slf4j
@CommandLine.Command(name = "history")
class ConversationsHistory implements Callable<Integer> {

    public static final int DEFAULT_LIMIT = 200;

    @Option(names = {"-c", "--channel"}, required = true)
    private String channelId;

    @Option(names = {"-s", "--start"})
    private LocalDateTime oldest;

    @Option(names = {"-e", "--end"})
    private LocalDateTime latest;

    @Option(names = {"-o", "--output"})
    private Path output;

    @ParentCommand
    private App app;

    @Override
    public Integer call() {
        return fetchHistory(
                app.slackToken,
                channelId,
                new TimeRange.TimeRangeBuilder().from(oldest).to(latest).build()
        );
    }


    int fetchHistory(String token, String channel, TimeRange timeRange) {
        var client = Slack.getInstance().methods();

        var messages = new ArrayList<Message>();
        var hasMoreResults = false;
        var nextCursor = new AtomicReference<>("");

        do {
            try {
                var result = client.conversationsHistory(r -> r
                        .token(token)
                        .channel(channel)
                        .oldest(timeRange.from())
                        .latest(timeRange.to())
                        .limit(DEFAULT_LIMIT)
                        .cursor(nextCursor.get())
                );

                if (!result.isOk()) {
                    log.error("Slack Web API failure. Error: '{}'", result.getError());
                    return -1;
                }

                messages.addAll(Optional.ofNullable(result.getMessages()).orElse(emptyList()));


                if (result.isHasMore()) {
                    hasMoreResults = true;
                    nextCursor.set(result.getResponseMetadata().getNextCursor());
                } else {
                    hasMoreResults = false;
                    nextCursor.set("");
                }

            } catch (IOException | SlackApiException e) {
                log.error("Error while fetching history: {}", e.getMessage(), e);
                return -1;
            }
        } while (hasMoreResults);

        log.info("Fetched {} messages", messages.size());
        log.info("Filtering out messages ...");

        var filtered = messages.stream()
                .filter(message -> {
                    var string = message.getSubtype();
                    return string == null || string.isEmpty();
                })
                .filter(message -> !message.isHidden())
                .filter(message -> !message.isIntro())
                .toList();

        return printOrSave(filtered, output);
    }

    private int printOrSave(List<Message> messages, Path output) {

        var gson = GsonFactory.createSnakeCase();
        var messagesJson = gson.toJson(messages);

        if (output == null) {
            log.info("--- Output ---\n{}", messagesJson);
            return 0;
        }

        try {
            Files.writeString(output, gson.toJson(messages));
            log.info("Saved {} messages to {}", messages.size(), output.toRealPath());
            return 0;
        } catch (IOException e) {
            log.error("Error while saving history: {}", e.getMessage(), e);
            return -1;
        }
    }
}
