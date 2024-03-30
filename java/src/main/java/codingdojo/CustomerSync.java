package codingdojo;

import codingdojo.datawriter.DataWriter;
import codingdojo.datawriter.DataWriter.ACTION;
import codingdojo.dataloader.DataLoader;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CustomerSync {

  private final DataLoader dataLoader;
  private final DataWriter dataWriter;


  public CustomerSync(DataLoader dataLoader, DataWriter dataWriter) {
    this.dataLoader = dataLoader;
    this.dataWriter = dataWriter;
  }

  public ACTION syncWithDataLayer(ExternalCustomer externalCustomer) {
    //load data from the data layer
    CustomerMatches customerMatches = dataLoader.load(externalCustomer);

    // sync the data
    Customer customer = customerMatches.customer().orElse(buildCustomerIfNoMatch(externalCustomer));
    Customer newCustomer = populateFields(externalCustomer, customer);
    Collection<Customer> duplicates = syncDuplicateWithExternalCustomerName(externalCustomer, customerMatches.duplicates());

    // update the data layer
    dataWriter.write(duplicates);
    return dataWriter.write(newCustomer);
  }

  private static ImmutableCustomer buildCustomerIfNoMatch(ExternalCustomer externalCustomer) {
    return ImmutableCustomer.builder()
      .externalId(externalCustomer.externalId())
      .masterExternalId(externalCustomer.externalId())
      .customerType(externalCustomer.isCompany() ? CustomerType.COMPANY : CustomerType.PERSON)
      .build();
  }


  private static List<Customer> syncDuplicateWithExternalCustomerName(ExternalCustomer externalCustomer,
    Collection<Customer> duplicates) {
    return duplicates.stream()
      .map(duplicate -> ImmutableCustomer.copyOf(duplicate).withName(externalCustomer.name()))
      .collect(Collectors.toList());
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

  private static List<ShoppingList> mergeExternalCustomerListWithCustomer(ExternalCustomer externalCustomer, Customer customer) {
    return Stream.concat(
      customer.shoppingLists().stream(),
      externalCustomer.shoppingLists().stream()).collect(Collectors.toList());
  }
}
