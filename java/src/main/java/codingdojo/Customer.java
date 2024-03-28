package codingdojo;

import org.immutables.value.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Value.Immutable
public abstract class Customer {
    public abstract Optional<String> externalId();
    public abstract Optional<String> masterExternalId();
    public abstract Optional<Address> address();
    public abstract Optional<String> preferredStore();
    public abstract List<ShoppingList> shoppingLists();
    public abstract Optional<String> internalId();
    public abstract Optional<String> name();
    public abstract CustomerType customerType();
    public abstract Optional<String> companyNumber();


    public boolean isInternal() {
        return internalId().isPresent();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer)) return false;
        Customer customer = (Customer) o;
        return Objects.equals(externalId(), customer.externalId()) &&
                Objects.equals(masterExternalId(), customer.masterExternalId()) &&
                Objects.equals(companyNumber(), customer.companyNumber());
    }

    @Override
    public int hashCode() {
        return Objects.hash(externalId(), masterExternalId(), companyNumber());
    }
}
