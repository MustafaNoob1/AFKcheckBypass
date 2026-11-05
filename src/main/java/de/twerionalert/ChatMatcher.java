package de.twerionalert;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** Matches sender headers, never just a name mentioned in the message body. */
public final class ChatMatcher {
    private static final String NAME = "([A-Za-z0-9_]{3,16})";
    private static final String PREFIX = "(?:\\[[^\\]\\r\\n]{1,32}\\]\\s*){0,3}(?:[A-Za-z0-9_+]{1,20}\\s*[|┃]\\s*)?";
    private static final String BADGE = "(?:[\\p{So}\\p{Co}]+\\s*)?";
    private static final List<Pattern> FORMATS = List.of(
        Pattern.compile("^MSG\\s*[»›]\\s*" + NAME + "\\s*[»›]\\s*Dir\\s*:\\s*.+$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^<" + NAME + ">\\s+.+$"),
        Pattern.compile("^" + PREFIX + BADGE + NAME + "\\s*[:»›]\\s*.+$"),
        Pattern.compile("^\\[(?:MSG\\s*[:|]\\s*)?" + NAME + "\\s*(?:->|→|»|┃)\\s*(?:mir|dir|me|you)\\]\\s*[:»]?\\s*.+$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^(?:\\[MSG\\]\\s*)?" + NAME + "\\s*(?:->|→)\\s*(?:mir|dir|me|you)\\s*[:»]\\s*.+$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^" + NAME + "\\s+(?:flüstert dir|whispers to you)\\s*:\\s*.+$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^(?:Von|From)\\s+" + NAME + "\\s*:\\s*.+$", Pattern.CASE_INSENSITIVE)
    );

    public static Optional<String> sender(String message, List<String> watched) {
        String plain = message.replaceAll("§[0-9a-fk-orxA-FK-ORX]", "").strip();
        for (Pattern format : FORMATS) {
            var match = format.matcher(plain);
            if (!match.matches()) continue;
            String sender = match.group(1);
            return watched.stream().filter(name -> name.equalsIgnoreCase(sender)).findFirst();
        }
        return Optional.empty();
    }

    public static boolean isAllowedServer(String address, String allowedHost) {
        // Server addresses use an optional port. No DNS lookup or suffix substring matching.
        String host = address.strip().toLowerCase(java.util.Locale.ROOT).replaceFirst(":\\d+$", "");
        String allowed = allowedHost.strip().toLowerCase(java.util.Locale.ROOT);
        return !allowed.isEmpty() && (host.equals(allowed) || host.endsWith("." + allowed));
    }
}
