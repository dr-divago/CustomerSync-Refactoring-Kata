package codingdojo;

import org.immutables.value.Value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

@Value.Immutable
public abstract class CustomerMatches {
  public abstract Collection<Customer> duplicates();
  public abstract Optional<String> matchTerm();
  public abstract Optional<Customer> customer();


  public boolean matchedByExternalId() {
    return matchTerm().isPresent() && matchTerm().get().equals("ExternalId");
  }

  public boolean matchedByCompanyNumber() {
    return matchTerm().isPresent() && matchTerm().get().equals("CompanyNumber");
  }

}
