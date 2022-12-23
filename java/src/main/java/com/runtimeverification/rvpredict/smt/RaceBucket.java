package com.runtimeverification.rvpredict.smt;

import com.runtimeverification.rvpredict.violation.Race;

import java.util.List;
import java.util.Optional;

class RaceBucket {
    private final String name;
    private final List<Race> races;
    private int nextRaceIndex;
    private boolean solved;

    RaceBucket(String name, List<Race> races) {
        this.name = name;
        this.races = races;
        this.nextRaceIndex = 0;
        this.solved = false;
    }

    Optional<Race> nextRace() {
        if (solved || nextRaceIndex >= races.size()) {
            return Optional.empty();
        }
        return Optional.of(races.get(nextRaceIndex++));
    }

    String getNameAndMarkAsSolved() {
        solved = true;
        return name;
    }

    String getName() {
        return name;
    }
}
