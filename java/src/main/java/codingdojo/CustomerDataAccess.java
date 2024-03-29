package codingdojo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CustomerDataAccess {

  public static final String EXTERNAL_ID = "ExternalId";
  public static final String COMPANY_NUMBER = "CompanyNumber";

  private final CustomerDataLayer customerDataLayer;

    public CustomerDataAccess(CustomerDataLayer customerDataLayer) {
        this.customerDataLayer = customerDataLayer;
    }

    public CustomerMatches loadCompanyCustomer(String externalId, String companyNumber) {
        Optional<Customer> customerMatchedByExternalId = this.customerDataLayer.findByExternalId(externalId);
        CustomerMatches matches = customerMatchedByExternalId
          .map( customer ->matchByExternalId(externalId, customer))
          .orElse(matchByCompany(companyNumber));

      matches.customer()
        .filter(c -> !CustomerType.COMPANY.equals(c.customerType()))
        .ifPresent(c -> { throw new ConflictException("Existing customer for externalCustomer " + externalId + " already exists and is not a company");});

      if (matches.matchedByExternalId()) {
        matches = CustomerDataAccess.matchDuplicate(companyNumber, matches);
      } else if (matches.matchedByCompanyNumber()) {
        matches = CustomerDataAccess.matchCompanyNumber(externalId, companyNumber, matches);
      }

      return matches;
    }

  public CustomerMatches loadPersonCustomer(String externalId) {
    Optional<Customer> matchByPersonalNumber = this.customerDataLayer.findByExternalId(externalId);
    return matchByPersonalNumber
      .map(customer -> toCustomerMatches(Optional.of(customer), Optional.of(EXTERNAL_ID),Collections.emptyList()))
      .orElseGet(() -> toCustomerMatches(Optional.empty(), Optional.empty(), Collections.emptyList()));
  }

  private CustomerMatches matchByExternalId(final String externalId, final Customer matchByExternalId) {
    Optional<Customer> matchByMasterId = this.customerDataLayer.findByMasterExternalId(externalId);
    List<Customer> duplicates = new ArrayList<>();
    matchByMasterId.ifPresent(duplicates::add);

    return toCustomerMatches(Optional.of(matchByExternalId), Optional.of(EXTERNAL_ID), duplicates);
  }

  private CustomerMatches matchByCompany(final String companyNumber) {
    Optional<Customer> matchByCompanyNumber = this.customerDataLayer.findByCompanyNumber(companyNumber);
    return matchByCompanyNumber
      .map(customer -> toCustomerMatches(Optional.of(customer), Optional.of(COMPANY_NUMBER), Collections.emptyList()))
      .orElseGet(() -> toCustomerMatches(Optional.empty(), Optional.empty(), Collections.emptyList()));
  }

  public static CustomerMatches matchDuplicate(final String companyNumber, CustomerMatches customerMatches) {
    Optional<String> maybeCustomerCompanyNumber = customerMatches.customer().get().companyNumber();
    final CustomerMatches finalCustomerMatches = customerMatches;
    return maybeCustomerCompanyNumber
      .filter(customerCompanyNumber -> !companyNumber.equals(customerCompanyNumber))
      .map(c -> {
        Collection<Customer> duplicate = finalCustomerMatches.duplicates();
        duplicate.add(finalCustomerMatches.customer().get());
        return ImmutableCustomerMatches.builder()
          .from(finalCustomerMatches)
          .customer(Optional.empty())
          .matchTerm(Optional.empty())
          .duplicates(duplicate)
          .build();
      }).orElse(ImmutableCustomerMatches.copyOf(customerMatches));
  }

  public static CustomerMatches matchCompanyNumber(final String externalId, final String companyNumber, CustomerMatches customerMatches) {
    String customerExternalId = customerMatches.customer().get().externalId().orElse("");
    if (!customerExternalId.isEmpty() && !externalId.equals(customerExternalId)) {
      throw new ConflictException("Existing customer for externalCustomer " + companyNumber + " doesn't match external id " + externalId + " instead found " + customerExternalId );
    }
    customerMatches = matchByCompany(externalId, customerMatches);
    return customerMatches;
  }

  private static CustomerMatches matchByCompany(final String externalId, CustomerMatches customerMatches) {
    Customer customer = ImmutableCustomer.copyOf(customerMatches.customer().get())
      .withExternalId(externalId)
      .withMasterExternalId(externalId);
    customerMatches = ImmutableCustomerMatches.copyOf(customerMatches).withCustomer(Optional.of(customer));
    return customerMatches;
  }

  private static CustomerMatches toCustomerMatches(Optional<Customer> customer, Optional<String> matchTerm, List<Customer> duplicates) {
    return ImmutableCustomerMatches.builder()
      .customer(customer)
      .matchTerm(matchTerm)
      .duplicates(duplicates)
      .build();
  }

    public Customer updateCustomerRecord(Customer customer) {
        return customerDataLayer.updateCustomerRecord(customer);
    }

    public Customer createCustomerRecord(Customer customer) {
        return customerDataLayer.createCustomerRecord(customer);
    }

    public Customer updateShoppingList(Customer customer, ShoppingList consumerShoppingList) {
        List<ShoppingList> newList = new ArrayList<>(customer.shoppingLists());
        newList.add(consumerShoppingList);
        Customer updateCustomer = ImmutableCustomer.copyOf(customer).withShoppingLists(newList);
        customerDataLayer.updateShoppingList(consumerShoppingList);
        customerDataLayer.updateCustomerRecord(updateCustomer);
        return updateCustomer;
    }


}
