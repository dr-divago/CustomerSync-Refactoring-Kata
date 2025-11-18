package codingdojo.domain;

import org.immutables.value.Value;

@Value.Immutable
public abstract class Address {
    public abstract String street();
    public abstract String city();
    public abstract String postalCode();
}
