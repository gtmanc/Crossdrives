package com.crossdrives.msgraph;
import static com.crossdrives.msgraph.MeSnippets.getMeSnippets;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

public class SnippetContent {
    public static final List<AbstractSnippet<?, ?>> ITEMS = new ArrayList<>();

    static {
        AbstractSnippet<?, ?>[][] baseSnippets = new AbstractSnippet<?, ?>[][]{
                //getContactsSnippets(),
                //getGroupsSnippets(),
                //getEventsSnippets(),
                getMeSnippets()
                //getMessageSnippets(),
                //getUsersSnippets(),
                //getDrivesSnippets()
        };

        for (AbstractSnippet<?, ?>[] snippetArray : baseSnippets) {
            Collections.addAll(ITEMS, snippetArray);
        }
    }

}
