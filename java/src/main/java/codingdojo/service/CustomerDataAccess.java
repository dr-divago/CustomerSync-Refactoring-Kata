package codingdojo.service;

import codingdojo.dao.CustomerDataLayer;
import codingdojo.domain.Customer;
import codingdojo.domain.CustomerMatches;
import codingdojo.domain.CustomerType;
import codingdojo.domain.ImmutableCustomer;
import codingdojo.domain.ImmutableCustomerMatches;
import codingdojo.domain.ShoppingList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;

/**
 * This class is responsible for loading and updating customer data.
 * It uses the CustomerDataLayer to interact with the database.
 */
public class CustomerDataAccess {

  private final CustomerDataLayer customerDataLayer;

  public CustomerDataAccess(final CustomerDataLayer customerDataLayer) {
    this.customerDataLayer = customerDataLayer;
  }
  /**
   * This method loads a company customer from the database.
   *
   * @param externalId The external ID of the customer.
   * @param companyNumber The company number of the customer.
   * @return A CustomerMatches object containing the customer and any duplicates.
   */

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
      .map(customer -> Pair.of(
                customer,
                findDuplicate(externalId, Collections.singletonList(customer))))
      .map(customerAndDuplicate -> buildCustomerMatchByExternalId(
                customerAndDuplicate.getLeft(),
                companyNumber,
                customerAndDuplicate.getRight()))
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

  private static CustomerMatches matchCompanyNumber(
      final String externalId,
      final String companyNumber,
      final Customer customerMatched) {

    customerMatched.externalId()
        .ifPresent(customerExternalId -> throwsIfConflict(
            externalId,
            companyNumber,
            customerExternalId));

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
      final String customerExternalId) {

    if (!externalId.equals(customerExternalId)) {
      throw new ConflictException(
            "Existing customer for externalCustomer " + companyNumber + " doesn't match external id "
              + externalId + " instead found " + customerExternalId);
    }
  }

  /**
   * This method loads a person customer from the database.
   * If the customer is not found, it will return an empty CustomerMatches object.
   * If the customer is found, it will return the customer.
   * If the customer is found, but is not a person, it will throw a ConflictException.
   *
   * @param externalId The external ID of the customer.
   * @return A CustomerMatches object containing the customer.
   */
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

  /**
   * This method creates a new customer record in the database.
   *
   * @param customer The customer to create.
   * @return The created customer.
   */
  public Customer createCustomerRecord(final Customer customer) {
    Customer newCustomer = ImmutableCustomer
        .copyOf(customer)
        .withInternalId("fake internalId");
    return customerDataLayer.createCustomerRecord(newCustomer);
  }

  /**
   * This method updates the shopping list of a customer.
   *
   * @param customer The customer to update.
   * @param consumerShoppingList The shopping list to update.
   * @return The updated customer.
   */
  public Customer updateShoppingList(
      final Customer customer,
      final List<ShoppingList> consumerShoppingList) {

    consumerShoppingList.forEach(customerDataLayer::updateShoppingList);
    return customerDataLayer.updateCustomerRecord(customer);
  }
}
