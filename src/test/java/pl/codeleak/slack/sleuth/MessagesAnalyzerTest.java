package pl.codeleak.slack.sleuth;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

class MessagesAnalyzerTest {

    MessagesAnalyzer analyzer = new MessagesAnalyzer();

    @Test
    void groupsByMentionedUsers() {
        var m1 = MessageFactory.withTextOnly("Mentioning two users: <@U000000> and <@U000001>!");
        var m2 = MessageFactory.withTextOnly("Mentioning the same user three times <@U000000>, <@U000000>, <@U000000>!");
        var m3 = MessageFactory.withTextOnly("""
                Mentioning the same user two times in a multiline message:
                Line #1: <@U000001>
                Line #2: <@U000001>

                Done.
                """);
        var m4 = MessageFactory.withTextOnly("Hello!");

        var result = analyzer.groupByMentionedUser(List.of(m1, m2, m3, m4));

        assertEquals(2, result.size());
        assertIterableEquals(List.of(m1, m2), result.get("U000000"));
        assertIterableEquals(List.of(m1, m3), result.get("U000001"));
    }

    @Test
    void groupsByPostingUsers() {
        var m1 = MessageFactory.postedBy("U000000", "Message #1");
        var m2 = MessageFactory.postedBy("U000001", "Message #2");
        var m3 = MessageFactory.postedBy("U000001", "Message #3");
        var m4 = MessageFactory.postedBy("U000001", "Message #4");

        var result = analyzer.groupByPostingUser(List.of(m1, m2, m3, m4));

        assertEquals(2, result.size());
        assertIterableEquals(List.of(m1), result.get("U000000"));
        assertIterableEquals(List.of(m2, m3, m4), result.get("U000001"));
    }

    @Test
    void groupsByTags() {
        var m1 = MessageFactory.withTextOnly("Mentioning two tags: #one and #two!");
        var m2 = MessageFactory.withTextOnly("Mentioning the same tag three times #one, #one, #one!");
        var m3 = MessageFactory.withTextOnly("""
                Mentioning the same tag two times in a multiline message:
                Line #1: #one
                Line #2: #one

                Done.
                """);
        var m4 = MessageFactory.withTextOnly("Channel mentions are not tags: #one #two #Ctwo #Cthree");

        var result = analyzer.groupByTags(List.of(m1, m2, m3, m4));

        assertEquals(2, result.size());
        assertIterableEquals(List.of(m1, m2, m3, m4), result.get("#one"));
        assertIterableEquals(List.of(m1, m4), result.get("#two"));
    }

    @Test
    void sortsByReactions() {
        var m1 = MessageFactory.withReactions("Score:0", 0, 0);
        var m2 = MessageFactory.withReactions("Score:2", 1, 1);
        var m3 = MessageFactory.withReactions("Score:3", 2, 1);
        var m4 = MessageFactory.withReactions("Score:4", 3, 1);

        var result = analyzer.sortByReactions(List.of(m2, m1, m4, m3));

        assertEquals(m4, result.get(0));
        assertEquals(m3, result.get(1));
        assertEquals(m2, result.get(2));
        assertEquals(m1, result.get(3));
    }
}
