package codingdojo.datawriter;

import codingdojo.dao.CustomerDataLayer;
import codingdojo.domain.Customer;
import codingdojo.domain.ImmutableCustomer;
import codingdojo.domain.ShoppingList;
import java.util.Collection;
import java.util.List;

public class DbDataWriter implements DataWriter {

  private final CustomerDataLayer customerDataLayer;

  public DbDataWriter(CustomerDataLayer customerDataLayer) {
    this.customerDataLayer = customerDataLayer;
  }

  @Override
  public ACTION write(Customer customer) {
    return customer.isInternal()
      ? updateCustomer(customer)
      : createCustomerRecord(customer);

  }

  @Override
  public void write(Collection<Customer> duplicates) {
      duplicates.stream()
        .filter(Customer::isInternal)
        .forEach(this.customerDataLayer::updateCustomerRecord);

      duplicates.stream()
        .filter(Customer::isNotInternal)
        .forEach(this.customerDataLayer::createCustomerRecord);
  }

  private ACTION updateCustomer(Customer customer) {
    this.updateCustomerRecord(customer);
    this.updateShoppingList(customer, customer.shoppingLists());
    return ACTION.UPDATE;
  }

  private ACTION createCustomerRecord(Customer customer) {
    Customer newCustomer = ImmutableCustomer
      .copyOf(customer)
      .withInternalId("fake internalId");
    customerDataLayer.createCustomerRecord(newCustomer);
    return ACTION.CREATE;
  }

  public Customer updateCustomerRecord(final Customer customer) {
    return customerDataLayer.updateCustomerRecord(customer);
  }

  public Customer updateShoppingList(
    final Customer customer,
    final List<ShoppingList> consumerShoppingList) {

    consumerShoppingList.forEach(customerDataLayer::updateShoppingList);
    return customerDataLayer.updateCustomerRecord(customer);
  }

}
