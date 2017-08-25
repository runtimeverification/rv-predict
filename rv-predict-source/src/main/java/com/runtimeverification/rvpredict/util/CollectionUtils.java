package com.runtimeverification.rvpredict.util;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

public class CollectionUtils {
    public static <T> List<T> toList(Optional<T> optional) {
        return optional.map(Collections::singletonList).orElseGet(Collections::emptyList);
    }
    public static List<Long> toList(OptionalLong optional) {
        return optional.isPresent() ? Collections.singletonList(optional.getAsLong()) : Collections.emptyList();
    }
    public static List<Integer> toList(OptionalInt optional) {
        return optional.isPresent() ? Collections.singletonList(optional.getAsInt()) : Collections.emptyList();
    }
}
