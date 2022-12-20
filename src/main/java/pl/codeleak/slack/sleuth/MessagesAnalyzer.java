package pl.codeleak.slack.sleuth;

import com.slack.api.model.Message;
import com.slack.api.model.Reaction;
import com.slack.api.model.User;
import com.slack.api.util.json.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

@Slf4j
@CommandLine.Command(name = "analyzer")
class MessagesAnalyzer implements Callable<Integer> {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @CommandLine.ParentCommand
    private App app;

    @CommandLine.Option(names = {"-i", "--input"}, required = true)
    private Path input;

    private int limit;

    @CommandLine.Option(names = {"-v", "--verbose"}, defaultValue = "false")
    private boolean verbose;

    @CommandLine.Option(names = {"-l", "--limit"}, defaultValue = "3")
    public void setLimit(int limit) {
        if (limit < 1 || limit > 100) {
            throw new CommandLine.ParameterException(spec.commandLine(),
                    "Value for `limit` must be in range of 1 to 100");
        }
        this.limit = limit;
    }

    private final UsersInfo usersInfo = new UsersInfo();
    private final ConcurrentHashMap<String, User> usersCache = new ConcurrentHashMap<>(0);

    @Override
    public Integer call() throws Exception {
        var gson = GsonFactory.createSnakeCase();
        var messagesArray = gson.fromJson(Files.readString(input), Message[].class);
        var messages = List.of(messagesArray);

        log.info("Analyzing {} message(s) extracted from {}", messages.size(), input.toAbsolutePath());
        log.info("Limiting statistics to [{}]", limit);

        var mentionedUsers = groupByMentionedUser(messages);
        if (mentionedUsers.size() > 0) {
            log.info("✅ Most mentioned users: ssss");
            mentionedUsers.entrySet()
                    .stream().limit(limit)
                    .forEachOrdered(entry -> {
                        log.info("  ℹ️ User [{}] appeared in [{}] message(s)", userInfo(entry.getKey()), entry.getValue().size());
                        if (verbose) {
                            entry.getValue().forEach(message -> log.info("    💬 [{}]", normalize(message.getText())));
                        }
                    });
        }

        var postingUsers = groupByPostingUser(messages);
        if (postingUsers.size() > 0) {
            log.info("✅ Most posting users: ssss");
            postingUsers.entrySet()
                    .stream().limit(limit)
                    .forEachOrdered(entry -> {
                        log.info("  ℹ️ User [{}] posted [{}] message(s)", userInfo(entry.getKey()), entry.getValue().size());
                        if (verbose) {
                            entry.getValue().forEach(message -> log.info("    💬 [{}]", normalize(message.getText())));
                        }
                    });
        }

        var mostReactedMessages = sortByReactions(messages);
        if (mostReactedMessages.size() > 0) {
            log.info("✅ Popular messages (based on reactions, replies and reply users count):");
            mostReactedMessages.stream()
                    .limit(limit)
                    .forEachOrdered(message -> log.info("  ℹ️ Reactions score [{}] for message 💬 [{}]", calculateReactionsScore(message), normalize(message.getText())));
        }

        var byTags = groupByTags(messages);
        if (byTags.size() > 0) {
            log.info("✅ Messages by tags (replies not included):");
            byTags.entrySet()
                    .stream().limit(limit)
                    .filter(entry -> entry.getValue().size() > 0)
                    .forEachOrdered(entry -> {
                        log.info("  ℹ️ Tag [{}] was used [{}] time(s)", entry.getKey(), entry.getValue().size());
                        if (verbose) {
                            entry.getValue().forEach(message -> log.info("    💬 [{}]", normalize(message.getText())));
                        }
                    });
        }

        return 0;
    }

    Map<String, List<Message>> groupByMentionedUser(List<Message> messages) {
        Map<String, List<Message>> result = new HashMap<>();
        for (Message message : messages) {
            for (String user : extractMentionedUsers(message.getText())) {
                result.merge(user, List.of(message), (oldValue, value) ->
                        Stream.concat(oldValue.stream(), value.stream()).toList());
            }
        }
        return sortByMessagesCount(result);
    }

    Map<String, List<Message>> groupByPostingUser(List<Message> messages) {
        Map<String, List<Message>> result = new HashMap<>();
        for (Message message : messages) {
            result.merge(message.getUser(), List.of(message), (oldValue, value) ->
                    Stream.concat(oldValue.stream(), value.stream()).toList());
        }

        return sortByMessagesCount(result);
    }

    Map<String, List<Message>> groupByTags(List<Message> messages) {
        Map<String, List<Message>> result = new HashMap<>();
        for (Message message : messages) {
            extractTags(message.getText()).forEach(tag -> result.merge(tag, List.of(message), (oldValue, value) ->
                    Stream.concat(oldValue.stream(), value.stream()).toList()));
        }
        return sortByMessagesCount(result);
    }

    LinkedHashMap<String, List<Message>> sortByMessagesCount(Map<String, List<Message>> input) {
        return input.entrySet()
                .stream()
                .sorted((o1, o2) -> o2.getValue().size() - o1.getValue().size())
                .collect(toMap(Map.Entry::getKey,
                        Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    List<Message> sortByReactions(List<Message> messages) {
        return messages
                .stream()
                .sorted((o1, o2) -> calculateReactionsScore(o2) - calculateReactionsScore(o1))
                .collect(Collectors.toList());
    }

    int calculateReactionsScore(Message message) {
        var reactionsCount = Optional.ofNullable(message.getReactions()).orElse(List.of())
                .stream()
                .mapToInt(Reaction::getCount)
                .sum();
        var replyCount = Optional.ofNullable(message.getReplyCount()).orElse(0);
        var replyUsersCount = Optional.ofNullable(message.getReplyUsersCount()).orElse(0);
        return reactionsCount + replyCount + replyUsersCount;
    }

    Set<String> extractMentionedUsers(String text) {
        return extractByRegex(text, "<@(U.+?)>");
    }

    Set<String> extractTags(String text) {
        return extractByRegex(text, "(#[^C]\\w+)");
    }

    private Set<String> extractByRegex(String text, String regex) {
        var results = new HashSet<String>();
        var pattern = Pattern.compile(regex);
        var matcher = pattern.matcher(text);
        while (matcher.find()) {
            results.add(matcher.group(1));
        }
        return results;
    }

    String userInfo(String user) {
        if (!verbose) {
            return user;
        }
        var userInfo = usersCache.computeIfAbsent(user, key -> usersInfo.fetchUserInfo(app.slackToken, key));
        if (userInfo != null) {
            return userInfo.getName() + " (" + userInfo.getRealName() + ")";
        }
        return user + " (!Not Found!)";
    }

    String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\r", " ")
                .replace("\n", " ")
                .replace("\t", " ");
    }
}
