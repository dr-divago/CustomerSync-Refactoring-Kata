package codingdojo;

import org.immutables.value.Value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

@Value.Immutable
public abstract class CustomerMatches {
  @Value.Parameter
  public abstract Optional<Customer> customer();
  @Value.Parameter
  public abstract Collection<Customer> duplicates();
}
