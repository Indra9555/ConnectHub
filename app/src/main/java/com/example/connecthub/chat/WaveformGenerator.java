package com.example.connecthub.chat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WaveformGenerator {

    public static List<Integer> generate(File file) {

        List<Integer> waveform = new ArrayList<>();

        long size = file.length();

        int bars = 60;

        for (int i = 0; i < bars; i++) {

            int value = (int) (
                    20 +
                            ((size / (i + 1)) % 80)
            );

            waveform.add(value);
        }

        return waveform;
    }
}