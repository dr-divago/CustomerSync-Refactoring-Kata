package codingdojo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CustomerDataAccess {

  private final CustomerDataLayer customerDataLayer;

  public CustomerDataAccess(CustomerDataLayer customerDataLayer) {
    this.customerDataLayer = customerDataLayer;
  }

  public CustomerMatches loadCompanyCustomer(String externalId, String companyNumber) {
    Optional<Customer> maybeCustomerMatchedByExternalId = this.customerDataLayer.findByExternalId(
      externalId);

    maybeCustomerMatchedByExternalId
      .filter(Customer::isNotCompany)
      .ifPresent(c -> {
        throw new ConflictException("Existing customer for externalCustomer " + externalId
          + " already exists and is not a company");
      });

    return maybeCustomerMatchedByExternalId
      .map(customer -> matchByExternalId(externalId, customer, companyNumber, Arrays.asList(customer)))
      .orElse(matchByCompany(externalId, companyNumber));
  }

  private CustomerMatches matchByExternalId(final String externalId,
    final Customer matchByExternalId, String companyNumber, List<Customer> dup) {
    Optional<Customer> matchByMasterId = this.customerDataLayer.findByMasterExternalId(externalId);
    List<Customer> updatedDuplicate = updateDuplicate(dup, matchByMasterId);

    return matchByExternalId.companyNumber().get().equals(companyNumber)
      ? ImmutableCustomerMatches.of(Optional.of(matchByExternalId), updatedDuplicate)
      : ImmutableCustomerMatches.of(Optional.empty(), updatedDuplicate);
  }

  private static List<Customer> updateDuplicate(List<Customer> dup, Optional<Customer> matchByMasterId) {
    return matchByMasterId
      .map(c -> Stream.concat(dup.stream(), Stream.of(c)))
      .orElseGet(() -> dup.stream())
      .collect(Collectors.toList());
  }

  public CustomerMatches loadPersonCustomer(String externalId) {
    Optional<Customer> matchByPersonalNumber = this.customerDataLayer.findByExternalId(externalId);
    return matchByPersonalNumber
      .map(customer -> ImmutableCustomerMatches.of(Optional.of(customer), Collections.emptyList()))
      .orElseGet(() -> ImmutableCustomerMatches.of(Optional.empty(), Collections.emptyList()));
  }

  private CustomerMatches matchByCompany(final String externalId, final String companyNumber) {
    Optional<Customer> customerMatchByCompanyNumber = this.customerDataLayer.findByCompanyNumber(
      companyNumber);
    CustomerMatches matches = customerMatchByCompanyNumber
      .map(customer -> matchCompanyNumber(externalId, companyNumber, customer))
      .orElseGet(() -> ImmutableCustomerMatches.of(Optional.empty(), Collections.emptyList()));

    return matches;
  }

  public static CustomerMatches matchCompanyNumber(final String externalId,
    final String companyNumber, Customer customerMatched) {
    customerMatched.externalId()
      .filter(customer -> !externalId.equals(customer))
      .ifPresent(c -> {
        throw new ConflictException(
          "Existing customer for externalCustomer " + companyNumber + " doesn't match external id "
            + externalId + " instead found " + c);
      });
    return matchCompanyByExternalId(externalId, customerMatched);
  }

  private static CustomerMatches matchCompanyByExternalId(final String externalId,
    Customer customerMatches) {
    return ImmutableCustomerMatches.builder()
      .customer(ImmutableCustomer.copyOf(customerMatches)
        .withExternalId(externalId)
        .withMasterExternalId(externalId))
      .duplicates(Collections.emptyList())
      .build();
  }

  public Customer updateCustomerRecord(Customer customer) {
    return customerDataLayer.updateCustomerRecord(customer);
  }

  public Customer createCustomerRecord(Customer customer) {
    Customer newCustomer = ImmutableCustomer
      .copyOf(customer)
      .withInternalId("fake internalId");
    return customerDataLayer.createCustomerRecord(newCustomer);
  }

  public Customer updateShoppingList(Customer customer, List<ShoppingList> consumerShoppingList) {
    for (ShoppingList shoppingList : consumerShoppingList)
      customerDataLayer.updateShoppingList(shoppingList);
    return customerDataLayer.updateCustomerRecord(customer);
  }
}
