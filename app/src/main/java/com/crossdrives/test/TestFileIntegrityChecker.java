package com.crossdrives.test;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Scanner;

public class TestFileIntegrityChecker {
    final String TAG = "CD.TestFileIntegrityChecker";
    long length;
    InputStreamReader fis;
    Scanner scanner;

    public enum Pattern{
        PATTERN_SERIAL_NUM("Serial number");

        String name;
        Pattern(String name) {
            this.name = name;
        }
        String getName(){return name;}

    }

    HashMap<String, Rule> rules = new HashMap<>();

    public TestFileIntegrityChecker(long length, FileInputStream fis) {
        this.length = length;
        this.fis = new InputStreamReader(fis);
        scanner = new Scanner(fis);
        rules.put(Pattern.PATTERN_SERIAL_NUM.getName(), SerialNumberCheck);
    }

    public int execute(int chunkSize, Pattern pattern) throws IOException {

        return rules.get(pattern.getName()).check();
    }

    Rule SerialNumberCheck = new Rule() {
        @Override
        public int check() throws IOException {
            int i = 0;
            int seq = 0;
            int count = 0;
            String rd = null;
            boolean hasNext = false;
            boolean result = true;

            while(hasNext = scanner.hasNext()){
                i = scanner.nextInt();
                rd = Integer.toString(i);
                if(i%100 == 0){
                    Log.d(TAG, "Integrity check at position: " + i);
                }
//                if(count < 10 ){
//                    Log.d(TAG, "read: " + rd);
//                    count++;
//                }
                if(!Integer.toString(seq).equals(rd)){
                    Log.d(TAG, "Integrity check failed at position: " + rd);
                    result = false;
                    break;
                }
                seq++;
            }

            if(hasNext == false && result == true){
                Log.d(TAG, "Integrity check successfully.");
                seq = -1;
            }

            return seq;
        }
    };

    interface Rule{
        /*
            Return:
            Negative:
                EOF is reached. Check finishes successfully
            Zero and any of positive:
                Check ends unsuccessfully. The value indicates the sequence number where check is unsuccessful.
         */
        int check() throws IOException;
    }


}
