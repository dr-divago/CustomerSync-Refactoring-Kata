package codingdojo.domain;


import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public abstract class ExternalCustomer {
    public abstract Address address();
    public abstract String name();
    public abstract Optional<String> preferredStore();
    public abstract List<ShoppingList> shoppingLists();
    public abstract String externalId();
    public abstract Optional<String> companyNumber();
    public abstract Optional<Integer> bonusPoints();

  public boolean isCompany() {
    return companyNumber().isPresent();
  }

  public boolean isPerson() {return !isCompany(); }
}
