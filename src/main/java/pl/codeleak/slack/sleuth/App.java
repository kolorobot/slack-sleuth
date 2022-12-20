package pl.codeleak.slack.sleuth;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.*;

@Slf4j
@Command(name = "App", subcommands = {ConversationsList.class, ConversationsHistory.class, MessagesAnalyzer.class, UsersInfo.class})
class App {

    @Option(names = {"-t", "--token"}, required = true)
    protected String slackToken;

    public static void main(String[] args) {
        int result = new CommandLine(new App()).execute(args);
        System.exit(result);
    }
}
