package net.mailific.util;

import java.util.List;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.Test;

public class DistinguisherTest {

  Distinguisher<String> it;

  @Test
  public void distinct() {
    it = new Distinguisher<String>(String::toLowerCase);

    List<String> actual = it.distinct(Stream.of("aBc", "ABC", "abc", "aBc", "abcd"));

    MatcherAssert.assertThat(actual, IsIterableContainingInOrder.contains("aBc", "abcd"));
  }
}
