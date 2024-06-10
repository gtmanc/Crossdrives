package com.crossdrives.signin.microsoft;

import com.microsoft.identity.client.exception.MsalException;

public interface IAuthenticationHelperCreatedListener {
        void onCreated(final AuthenticationHelper authHelper);
        void onError(final MsalException exception);
}
