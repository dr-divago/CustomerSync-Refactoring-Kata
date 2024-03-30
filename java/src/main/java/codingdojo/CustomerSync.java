package codingdojo;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CustomerSync {

  private final CustomerDataAccess customerDataAccess;

  public CustomerSync(CustomerDataLayer customerDataLayer) {
    this(new CustomerDataAccess(customerDataLayer));
  }

  public CustomerSync(CustomerDataAccess db) {
    this.customerDataAccess = db;
  }

  public boolean syncWithDataLayer(ExternalCustomer externalCustomer) {

    CustomerMatches customerMatches = externalCustomer.isCompany()
        ? loadCompany(externalCustomer)
        : loadPerson(externalCustomer);

    Customer customer = customerMatches.customer().orElse(buildCustomerIfNoMatch(externalCustomer));

    Customer newCustomer = populateFields(externalCustomer, customer);
    Collection<Customer> duplicates = syncDuplicateWithExternalCustomerName(externalCustomer, customerMatches.duplicates());

    updateDuplicate(duplicates);

    return newCustomer.isInternal()
      ? updateCustomer(newCustomer)
      : createCustomerRecord(newCustomer);

  }

  private static ImmutableCustomer buildCustomerIfNoMatch(ExternalCustomer externalCustomer) {
    return ImmutableCustomer.builder()
      .externalId(externalCustomer.externalId())
      .masterExternalId(externalCustomer.externalId())
      .customerType(externalCustomer.isCompany() ? CustomerType.COMPANY : CustomerType.PERSON)
      .build();
  }

  private void updateDuplicate(Collection<Customer> duplicates) {
    duplicates.stream()
      .filter(Customer::isInternal)
      .forEach(this.customerDataAccess::updateCustomerRecord);

    duplicates.stream()
      .filter(Customer::isNotInternal)
      .forEach(c -> this.customerDataAccess.createCustomerRecord(c));
  }

  private static List<Customer> syncDuplicateWithExternalCustomerName(ExternalCustomer externalCustomer,
    Collection<Customer> duplicates) {
    return duplicates.stream()
      .map(duplicate -> ImmutableCustomer.copyOf(duplicate).withName(externalCustomer.name()))
      .collect(Collectors.toList());
  }

  private boolean createCustomerRecord(Customer newCustomer) {
    this.customerDataAccess.createCustomerRecord(newCustomer);
    return true;
  }

  private boolean updateCustomer(Customer customer) {
    this.customerDataAccess.updateCustomerRecord(customer);
    this.customerDataAccess.updateShoppingList(customer, customer.shoppingLists());
    return false;
  }

  private Customer populateFields(ExternalCustomer externalCustomer, Customer customer) {
    List<ShoppingList> mergedShoppingList = mergeExternalCustomerListWithCustomer(externalCustomer, customer);

    return ImmutableCustomer.copyOf(customer)
      .withName(externalCustomer.name())
      .withCompanyNumber(externalCustomer.companyNumber())
      .withCustomerType(externalCustomer.isCompany() ? CustomerType.COMPANY : CustomerType.PERSON)
      .withAddress(externalCustomer.address())
      .withPreferredStore(externalCustomer.preferredStore())
      .withShoppingLists(mergedShoppingList);
  }

  private static List<ShoppingList> mergeExternalCustomerListWithCustomer(
    ExternalCustomer externalCustomer,
    Customer customer) {
    return Stream.concat(
      customer.shoppingLists().stream(),
      externalCustomer.shoppingLists().stream()).collect(Collectors.toList());
  }

  public CustomerMatches loadCompany(ExternalCustomer externalCustomer) {
    return customerDataAccess.loadCompanyCustomer(externalCustomer.externalId(),
      externalCustomer.companyNumber().orElseThrow(IllegalStateException::new));
  }


  public CustomerMatches loadPerson(ExternalCustomer externalCustomer) {
    final String externalId = externalCustomer.externalId();

    CustomerMatches customerMatches = customerDataAccess.loadPersonCustomer(externalId);

    if (customerMatches.customer().isPresent()) {
      if (!CustomerType.PERSON.equals(customerMatches.customer().get().customerType())) {
        throw new ConflictException("Existing customer for externalCustomer " + externalId
          + " already exists and is not a person");
      }
    }

    return customerMatches;
  }
}
