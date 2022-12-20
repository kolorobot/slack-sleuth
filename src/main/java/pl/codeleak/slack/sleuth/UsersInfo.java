package pl.codeleak.slack.sleuth;

import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.model.User;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.IOException;
import java.util.concurrent.Callable;

@Slf4j
@CommandLine.Command(name = "user")
class UsersInfo implements Callable<Integer> {

    @CommandLine.ParentCommand
    private App app;

    @CommandLine.Option(names = {"-u", "--user"}, required = true)
    private String user;

    User fetchUserInfo(String token, String user) {
        var client = Slack.getInstance().methods();
        try {
            var result = client.usersInfo(r -> r
                    .token(token)
                    .user(user)
            );

            if (!result.isOk()) {
                throw new RuntimeException("Error while fetching user info for [" + user + "]. Error: [" + result.getError() + "]");
            }

            return result.getUser();

        } catch (IOException | SlackApiException e) {
            throw new RuntimeException("Error while fetching user info for [ " + user + " ]", e);
        }
    }

    @Override
    public Integer call() {
        try {
            var userInfo = fetchUserInfo(app.slackToken, user);
            var message = """
                    ℹ️ User info (id: %s):
                     - Name: %s
                     - Real name: %s
                    """.formatted(user, userInfo.getName(), userInfo.getRealName());

            log.info(message);
            return 0;
        } catch (Exception e) {
            log.error(e.getMessage());
            return -1;
        }
    }
}
