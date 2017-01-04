package com.diversityarrays.kdxplore.trialdesign;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.diversityarrays.kdxplore.design.EntryFactor;
import com.diversityarrays.kdxplore.design.EntryType;
import com.diversityarrays.kdxplore.design.EntryType.Variant;
import com.diversityarrays.util.Pair;

public class TestTrialEntry {


    static private List<Pair<EntryType,Double>> ENTRY_TYPE_PAIRS = new ArrayList<>();
    static private Map<EntryFactor,List<String>> FACTOR_VALUES_BY_FACTOR = new HashMap<>();
    static {
        Random random = new Random(0);
        FACTOR_VALUES_BY_FACTOR.put(new EntryFactor("Height"), createRandomStringsBetween(40, random, 23, 99));
        FACTOR_VALUES_BY_FACTOR.put(new EntryFactor("Weight"), createRandomStringsBetween(50, random, 120, 356));
        FACTOR_VALUES_BY_FACTOR.put(new EntryFactor("Colour"), Arrays.asList("Red", "Orange", "Yellow", "Green", "Blue", "Indigo", "Violet"));

        ENTRY_TYPE_PAIRS.add(new Pair<>(new EntryType("Check", Variant.CHECK), 0.1));
        ENTRY_TYPE_PAIRS.add(new Pair<>(new EntryType("Normal", Variant.ENTRY), 0.7));
        ENTRY_TYPE_PAIRS.add(new Pair<>(new EntryType("Dry", Variant.CHECK), 0.2));
    };

    static public TrialEntry createTrialEntry(int entryId, Random random) {
        return createTrialEntry(entryId, random, null);
    }

    static public TrialEntry createTrialEntry(int entryId, Random random, Integer reps)
    {
        Map<EntryFactor, String> factorMap = new HashMap<>();
        for (EntryFactor f : FACTOR_VALUES_BY_FACTOR.keySet()) {
            List<String> values = FACTOR_VALUES_BY_FACTOR.get(f);
            int index = random.nextInt(values.size());
            factorMap.put(f, values.get(index));
        }

        double d = random.nextDouble();
        double total = 0;
        EntryType entryType = ENTRY_TYPE_PAIRS.get(ENTRY_TYPE_PAIRS.size()-1).first;
        for (Pair<EntryType,Double> pair : ENTRY_TYPE_PAIRS) {
            total += pair.second;
            if (d <= total) {
                entryType = pair.first;
                break;
            }
        }

        int last3 = (int) (0xfff & System.nanoTime() % 1000L);
        String location = "";
        String experiment = "";
        String entryName = String.format("Name %d", last3);
        String nest = "";

        TrialEntry result = new TrialEntry(location, experiment,
                entryId, entryName,
                entryType, nest, factorMap);
        if (reps != null) {
            result.setReplication(reps);
        }
        return result;
    }

    static public List<TrialEntry> createTestEntries() {
        return createTestEntries(null);
    }

    static public List<TrialEntry> createTestEntries(Integer reps) {

        Random random = new Random(0);
        List<TrialEntry> result = new ArrayList<>();

        int entryId = 0;
        for (int i = 57; --i >= 1; ) {
            result.add(createTrialEntry(++entryId, random, reps));
        }

        return result;
    }


    static private List<String> createRandomStringsBetween(int count, Random random, int min, int max) {
        List<String> all = new ArrayList<>();
        for (int i = min; i <= max; ++i) {
            all.add(Integer.toString(i));
        }

        return getRandomSubset(count, random, all);
    }

    static private List<String> getRandomSubset(int count, Random random, List<String> all) {
        List<String> result = new ArrayList<>();
        for (int i = count; --i >= 0; ) {
            int index = random.nextInt(all.size());
            result.add(all.remove(index));
        }

        return result;
    }

}
