package codingdojo.service;

import codingdojo.dataloader.DataLoader;
import codingdojo.datawriter.DataWriter;
import codingdojo.datawriter.DataWriter.ACTION;
import codingdojo.domain.Customer;
import codingdojo.domain.CustomerMatches;
import codingdojo.domain.CustomerType;
import codingdojo.domain.ExternalCustomer;
import codingdojo.domain.ImmutableCustomer;
import codingdojo.domain.ShoppingList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is responsible for syncing the data between the external customer and the
 * internal customer.
 */
public class CustomerSync {

  private final DataWriter dataWriter;
  private final DataLoader dataLoader;


  public CustomerSync(final DataLoader dataLoader, final DataWriter dataWriter) {
    this.dataWriter = dataWriter;
    this.dataLoader = dataLoader;
  }

  /**
   * This method syncs the data between the external customer and the internal customer.
   *
   * @param externalCustomer The external customer to sync with the internal customer.
   * @return The action taken to sync the data.
   */
  public ACTION syncWithDataLayer(final ExternalCustomer externalCustomer) {
    //load data from the data layer
    final CustomerMatches customerMatches = dataLoader.load(externalCustomer);

    // sync the data
    final Customer customer = customerMatches.customer()
        .orElse(buildCustomerIfNoMatch(externalCustomer));
    final Customer syncCustomer = syncFieldWithExternalCustomer(externalCustomer, customer);
    final Collection<Customer> duplicates = syncDuplicateWithExternalCustomerName(
        externalCustomer,
        customerMatches.duplicates());

    // update the data layer
    dataWriter.write(duplicates);
    return dataWriter.write(syncCustomer);
  }

  private static Customer buildCustomerIfNoMatch(final ExternalCustomer externalCustomer) {
    return ImmutableCustomer.builder()
      .externalId(externalCustomer.externalId())
      .masterExternalId(externalCustomer.externalId())
      .customerType(externalCustomer.isCompany() ? CustomerType.COMPANY : CustomerType.PERSON)
      .build();
  }


  private static List<Customer> syncDuplicateWithExternalCustomerName(
      final ExternalCustomer externalCustomer,
      final Collection<Customer> duplicates) {
    return duplicates.stream()
      .map(duplicate -> ImmutableCustomer.copyOf(duplicate).withName(externalCustomer.name()))
      .collect(Collectors.toList());
  }


  private Customer syncFieldWithExternalCustomer(
      final ExternalCustomer externalCustomer,
      final Customer customer) {

    final List<ShoppingList> mergedShoppingList =
        mergeExternalCustomerListWithCustomer(externalCustomer, customer);

    return ImmutableCustomer.copyOf(customer)
      .withName(externalCustomer.name())
      .withCompanyNumber(externalCustomer.companyNumber())
      .withCustomerType(externalCustomer.isCompany() ? CustomerType.COMPANY : CustomerType.PERSON)
      .withAddress(externalCustomer.address())
      .withPreferredStore(externalCustomer.preferredStore())
      .withBonusPoints(externalCustomer.isCompany()
        ? Optional.empty()
        : externalCustomer.bonusPoints())
      .withShoppingLists(mergedShoppingList);
  }

  private static List<ShoppingList> mergeExternalCustomerListWithCustomer(
      final ExternalCustomer externalCustomer,
      final Customer customer) {
    return Stream.concat(
      customer.shoppingLists().stream(),
      externalCustomer.shoppingLists().stream()).collect(Collectors.toList());
  }
}
