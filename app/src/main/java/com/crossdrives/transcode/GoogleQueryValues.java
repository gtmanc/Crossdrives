package com.crossdrives.transcode;

import java.util.ArrayList;
import java.util.List;

public class GoogleQueryValues extends BaseValue{
<<<<<<< HEAD
    public static String FOLDER = "application/vnd.google-apps.folder";
=======
    public static String FOLDER = "'application/vnd.google-apps.folder'";
>>>>>>> 983262f (#21 infrastructure build process)

    List<String> mValues = new ArrayList<>();

    public GoogleQueryValues() {
        super();

        mValues.add(FOLDER);
        setValues(mValues);
    }
}
