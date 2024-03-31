package codingdojo.domain;

import org.immutables.value.Value;

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
    public abstract Optional<Integer> bonusPoints();


    public boolean isInternal() {
        return internalId().isPresent();
    }

    public boolean isNotInternal() {
        return !isInternal();
    }

    public boolean isPerson() {
        return customerType().equals(CustomerType.PERSON);
    }

    public boolean isNotPerson() {
        return !isPerson();
    }

    public boolean isCompany() {
        return customerType().equals(CustomerType.COMPANY);
    }

    public boolean isNotCompany() {
        return !isCompany();
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
