package com.crossdrives.signin.microsoft;

import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;

import java.util.concurrent.CompletableFuture;

import okhttp3.Request;

public class GraphHelper {
    static GraphHelper INSTANCE = null;
    private final GraphServiceClient<Request> mClient;
    private GraphHelper() {
        AuthenticationHelper authProvider = AuthenticationHelper.getInstance();

        mClient = GraphServiceClient.builder()
                .authenticationProvider(authProvider).buildClient();
    }
    public static synchronized GraphHelper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GraphHelper();
        }

        return INSTANCE;
    }

    public CompletableFuture<User> getUser() {
        // GET /me (logged in user)
        return mClient.me().buildRequest()
                .select("displayName,mail,mailboxSettings,userPrincipalName")
                .getAsync();
    }
}
