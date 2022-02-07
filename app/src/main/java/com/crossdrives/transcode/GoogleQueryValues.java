package com.crossdrives.transcode;

import java.util.ArrayList;
import java.util.List;

public class GoogleQueryValues extends BaseValue{
    public static String FOLDER = "'application/vnd.google-apps.folder'";

    List<String> mValues = new ArrayList<>();

    public GoogleQueryValues() {
        super();

        mValues.add(FOLDER);
        setValues(mValues);
    }
}
