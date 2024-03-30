package codingdojo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;

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
      .map(customer -> Pair.of(customer, mergeDuplicate(externalId, Arrays.asList(customer))))
      .map(customerDupPair -> buildCustomerMatch(customerDupPair.getLeft(), companyNumber, customerDupPair.getRight()))
      .orElse(matchByCompany(externalId, companyNumber));
  }

  private List<Customer> mergeDuplicate(String externalId, List<Customer> dup) {
    return this.customerDataLayer.findByMasterExternalId(externalId)
      .map(duplicateCustomer -> mergeDuplicate(dup, duplicateCustomer))
      .orElse(dup);
  }

  private CustomerMatches buildCustomerMatch(final Customer matchByExternalId, String companyNumber, List<Customer> mergedDuplicate) {
    return matchByExternalId.companyNumber().get().equals(companyNumber)
      ? ImmutableCustomerMatches.of(Optional.of(matchByExternalId), mergedDuplicate)
      : ImmutableCustomerMatches.of(Optional.empty(), mergedDuplicate);
  }

  private static List<Customer> mergeDuplicate(List<Customer> dup, Customer matchByMasterId) {
    return Stream.concat(dup.stream(), Stream.of(matchByMasterId)).collect(Collectors.toList());
  }

  public CustomerMatches loadPersonCustomer(String externalId) {
    Optional<Customer> matchByPersonalNumber = this.customerDataLayer.findByExternalId(externalId);

    throwsIfConflict(externalId, matchByPersonalNumber);
    return matchByPersonalNumber
      .map(customer -> ImmutableCustomerMatches.of(Optional.of(customer), Collections.emptyList()))
      .orElseGet(() -> ImmutableCustomerMatches.of(Optional.empty(), Collections.emptyList()));
  }

  private static void throwsIfConflict(String externalId, Optional<Customer> matchByPersonalNumber) {
    if (matchByPersonalNumber.isPresent()) {
      if (!CustomerType.PERSON.equals(matchByPersonalNumber.get().customerType())) {
        throw new ConflictException("Existing customer for externalCustomer " + externalId
          + " already exists and is not a person");
      }
    }
  }

  private CustomerMatches matchByCompany(final String externalId, final String companyNumber) {
    Optional<Customer> customerMatchByCompanyNumber = this.customerDataLayer.findByCompanyNumber(
      companyNumber);
    return customerMatchByCompanyNumber
      .map(customer -> matchCompanyNumber(externalId, companyNumber, customer))
      .orElseGet(() -> ImmutableCustomerMatches.of(Optional.empty(), Collections.emptyList()));
  }

  public static CustomerMatches matchCompanyNumber(final String externalId,
    final String companyNumber, Customer customerMatched) {

    throwsIfConflict(externalId, companyNumber, customerMatched);
    return ImmutableCustomerMatches.builder()
      .customer(ImmutableCustomer.copyOf(customerMatched)
        .withExternalId(externalId)
        .withMasterExternalId(externalId))
      .duplicates(Collections.emptyList())
      .build();
  }

  private static void throwsIfConflict(String externalId, String companyNumber, Customer customerMatched) {
    customerMatched.externalId()
      .filter(customer -> !externalId.equals(customer))
      .ifPresent(c -> {
        throw new ConflictException(
          "Existing customer for externalCustomer " + companyNumber + " doesn't match external id "
            + externalId + " instead found " + c);
      });
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
