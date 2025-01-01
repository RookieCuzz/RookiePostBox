package com.cuzz.rookiepostbox.menu;

import nl.odalitadevelopments.menus.patterns.IteratorPattern;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ExamplePattern implements IteratorPattern {

    @Override
    public @NotNull List<@NotNull String> getPattern() {
        return List.of(
                "##|01|02|03|04|05|06|07|##",
                "##|08|09|10|11|12|13|14|##",
                "##|15|16|17|18|19|20|21|##",
                "##|22|23|24|25|26|27|28|##"
        );
    }

}