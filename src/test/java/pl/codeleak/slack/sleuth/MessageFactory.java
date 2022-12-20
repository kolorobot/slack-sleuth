package pl.codeleak.slack.sleuth;

import com.slack.api.model.Message;

class MessageFactory {

    static Message withTextOnly(String text) {
        var m = new Message();
        m.setText(text);
        return m;
    }

    static Message postedBy(String user, String text) {
        var m = new Message();
        m.setUser(user);
        m.setText(text);
        return m;
    }

    static Message withReactions(String text, int replyCount, int replyUsersCount) {
        var m = new Message();
        m.setText(text);
        m.setReplyCount(replyCount);
        m.setReplyUsersCount(replyUsersCount);
        return m;
    }
}
