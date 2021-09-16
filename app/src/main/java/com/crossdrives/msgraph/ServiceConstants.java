package com.crossdrives.msgraph;

/*
 * Copyright (c) Microsoft. All rights reserved. Licensed under the MIT license.
 * See LICENSE in the project root for license information.
 */

public class ServiceConstants {
    public static final String AUTHENTICATION_RESOURCE_ID = "https://graph.microsoft.com";
    public static final String AUTHORITY_URL = "https://login.microsoftonline.com/common";
    public static final String CLIENT_ID = "ENTER_YOUR_CLIENT_ID";
    public static final String[] SCOPES = {"openid", "Directory.ReadWrite.All","Calendars.ReadWrite","Files.ReadWrite","Mail.ReadWrite","Mail.Send","User.ReadBasic.All", "Group.ReadWrite.All", "profile"};
}
