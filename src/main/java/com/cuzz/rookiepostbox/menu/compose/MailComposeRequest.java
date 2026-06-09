package com.cuzz.rookiepostbox.menu.compose;

import java.util.List;
import java.util.UUID;

public record MailComposeRequest(
        MailComposeMode mode,
        UUID senderUuid,
        String senderName,
        List<MailComposeRecipient> recipients,
        String message,
        String adminNote
) {
}
