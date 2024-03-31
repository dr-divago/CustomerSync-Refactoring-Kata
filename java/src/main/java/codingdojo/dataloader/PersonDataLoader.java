package codingdojo.dataloader;

import codingdojo.dao.CustomerDataLayer;
import codingdojo.domain.Customer;
import codingdojo.domain.CustomerMatches;
import codingdojo.domain.CustomerType;
import codingdojo.domain.ExternalCustomer;
import codingdojo.domain.ImmutableCustomerMatches;
import codingdojo.service.ConflictException;
import java.util.Collections;
import java.util.Optional;

public class PersonDataLoader implements DataLoader {

  private final CustomerDataLayer customerDataLayer;

  public PersonDataLoader(CustomerDataLayer customerDataLayer) {
    this.customerDataLayer = customerDataLayer;
  }

  @Override
  public CustomerMatches load(ExternalCustomer externalCustomer) {

      Optional<Customer> matchByPersonalNumber = this.customerDataLayer.findByExternalId(
        externalCustomer.externalId());

      return matchByPersonalNumber
        .map(customer -> matchByPersonalNumber(customer, externalCustomer.externalId()))
        .orElseGet(() -> ImmutableCustomerMatches.of(Optional.empty(), Collections.emptyList()));
    }

    private static ImmutableCustomerMatches matchByPersonalNumber(final Customer customer, final String externalId) {
      throwsIfConflict(externalId, customer.customerType());
      return ImmutableCustomerMatches.of(Optional.of(customer), Collections.emptyList());
    }

    private static void throwsIfConflict(
    final String externalId,
    final CustomerType type) {

      if (!CustomerType.PERSON.equals(type)) {
        throw new ConflictException("Existing customer for externalCustomer " + externalId
          + " already exists and is not a person");
      }
    }
}
