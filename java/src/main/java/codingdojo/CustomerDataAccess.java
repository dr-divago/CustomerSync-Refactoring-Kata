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

  public CustomerDataAccess(final CustomerDataLayer customerDataLayer) {
    this.customerDataLayer = customerDataLayer;
  }

  public CustomerMatches loadCompanyCustomer(final String externalId, final String companyNumber) {
    Optional<Customer> maybeCustomerMatchedByExternalId = this.customerDataLayer.findByExternalId(
      externalId);

    maybeCustomerMatchedByExternalId
      .filter(Customer::isNotCompany)
      .ifPresent(c -> {
        throw new ConflictException("Existing customer for externalCustomer " + externalId
          + " already exists and is not a company");
      });

    return maybeCustomerMatchedByExternalId
      .map(customer -> Pair.of(customer, findDuplicate(externalId, Arrays.asList(customer))))
      .map(customerAndDuplicate -> buildCustomerMatchByExternalId(customerAndDuplicate.getLeft(), companyNumber, customerAndDuplicate.getRight()))
      .orElse(buildCustomerMatchByCompany(externalId, companyNumber));
  }

  private List<Customer> findDuplicate(
    final String externalId,
    final List<Customer> dup) {
    return
      findDuplicateCustomer(externalId)
        .map(duplicateCustomer -> mergeDuplicate(duplicateCustomer, dup))
        .orElse(dup);
  }

  private Optional<Customer> findDuplicateCustomer(final String externalId) {
    return this.customerDataLayer.findByMasterExternalId(externalId);
  }

  private static List<Customer> mergeDuplicate(
    final Customer matchByMasterId,
    final List<Customer> dup) {
    return Stream.concat(dup.stream(), Stream.of(matchByMasterId)).collect(Collectors.toList());
  }

  private CustomerMatches buildCustomerMatchByExternalId(
    final Customer matchByExternalId,
    final String companyNumber,
    final List<Customer> mergedDuplicate) {
    return matchByExternalId.companyNumber().get().equals(companyNumber)
      ? ImmutableCustomerMatches.of(Optional.of(matchByExternalId), mergedDuplicate)
      : ImmutableCustomerMatches.of(Optional.empty(), mergedDuplicate);
  }

  private CustomerMatches buildCustomerMatchByCompany(
    final String externalId,
    final String companyNumber) {
    return this.customerDataLayer.findByCompanyNumber(companyNumber)
      .map(customer -> matchCompanyNumber(externalId, companyNumber, customer))
      .orElse(ImmutableCustomerMatches.of(Optional.empty(), Collections.emptyList()));
  }

  public static CustomerMatches matchCompanyNumber(
    final String externalId,
    final String companyNumber,
    final Customer customerMatched) {

    throwsIfConflict(externalId, companyNumber, customerMatched.externalId());
    return ImmutableCustomerMatches.builder()
      .customer(ImmutableCustomer.copyOf(customerMatched)
        .withExternalId(externalId)
        .withMasterExternalId(externalId))
      .duplicates(Collections.emptyList())
      .build();
  }

  private static void throwsIfConflict(
    final String externalId,
    final String companyNumber,
    final Optional<String> customerExternalIdOptional) {

    customerExternalIdOptional
      .filter(customerExternalId -> !externalId.equals(customerExternalId))
      .ifPresent(c -> {
        throw new ConflictException(
          "Existing customer for externalCustomer " + companyNumber + " doesn't match external id "
            + externalId + " instead found " + c);
      });
  }

  public CustomerMatches loadPersonCustomer(final String externalId) {
    Optional<Customer> matchByPersonalNumber = this.customerDataLayer.findByExternalId(externalId);

    return matchByPersonalNumber
      .map(customer -> matchByPersonalNumber(customer, externalId))
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

  public Customer updateCustomerRecord(final Customer customer) {
    return customerDataLayer.updateCustomerRecord(customer);
  }

  public Customer createCustomerRecord(final Customer customer) {
    Customer newCustomer = ImmutableCustomer
      .copyOf(customer)
      .withInternalId("fake internalId");
    return customerDataLayer.createCustomerRecord(newCustomer);
  }

  public Customer updateShoppingList(
    final Customer customer,
    final List<ShoppingList> consumerShoppingList) {

    for (ShoppingList shoppingList : consumerShoppingList)
      customerDataLayer.updateShoppingList(shoppingList);
    return customerDataLayer.updateCustomerRecord(customer);
  }
}
