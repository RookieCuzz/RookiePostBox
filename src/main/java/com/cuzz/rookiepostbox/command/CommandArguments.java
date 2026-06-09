package com.cuzz.rookiepostbox.command;

import java.util.Arrays;

final class CommandArguments {

    private CommandArguments() {
    }

    static String joinFrom(String[] args, int startIndex) {
        return String.join(" ", Arrays.copyOfRange(args, startIndex, args.length)).trim();
    }
}
