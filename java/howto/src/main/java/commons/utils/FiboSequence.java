package commons.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FiboSequence {
  private static List<Integer> SEQUENCE = Arrays.asList(
    1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987, 1597, 2584, 4181, 6765, 10946, 17711);

  public static int next(int now) {
    return next(now, SEQUENCE.get(SEQUENCE.size()-1));
  }

  public static int next(int now, int max) {
    int pos = Collections.binarySearch(SEQUENCE, now);
    if (pos < 0) pos = -(pos+1);
    else pos++;

    if (pos >= SEQUENCE.size()) pos = SEQUENCE.size()-1;

    int t = SEQUENCE.get(pos);
    return t >= max ? max : t;
  }
}
