package com.crossdrives.test;

import java.io.File;
import java.util.HashMap;

public class TestFileIntegrityChecker {
    long length;
    File file;

    public enum Pattern{
        PATTERN_SERIAL_NUM("Serial number");

        String name;
        Pattern(String name) {
            this.name = name;
        }

    }

    static HashMap<String, Rule> rules = new HashMap<>();

    static {
        rules.put(Pattern.PATTERN_SERIAL_NUM, SerialNumberCheck);
    }

    public TestFileIntegrityChecker(long length, File file) {
        this.length = length;
        this.file = file;
    }

    public int execute(int chunkSize, Pattern pattern){
return 0;

    }

    interface Rule{
        boolean check();
    }

    static Rule SerialNumberCheck = new Rule() {
        @Override
        public boolean check() {
            return false;
        }
    };
}
