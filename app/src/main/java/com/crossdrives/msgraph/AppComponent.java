package com.crossdrives.msgraph;

import dagger.Component;

@Component(
        modules = {AppModule.class}
)
public interface AppComponent {
    SnippetApp injectApp(SnippetApp app);
}
