package codingdojo;


import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
public abstract class ExternalCustomer {
    public abstract Address address();
    public abstract String name();
    public abstract Optional<String> preferredStore();
    public abstract List<ShoppingList> shoppingLists();
    public abstract String externalId();
    public abstract Optional<String> companyNumber();

  public boolean isCompany() {
    return companyNumber().isPresent();
  }
}
