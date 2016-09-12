package org.icgc.dcc.dev.server.util;

import static lombok.AccessLevel.PRIVATE;

import java.util.function.Predicate;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public final class Predicates {

  public static <T> Predicate<T> isNotNull() {
    return x -> x != null;
  }
  
  public static <T> Predicate<T> isNull() {
    return x -> x != null;
  }
  
}
