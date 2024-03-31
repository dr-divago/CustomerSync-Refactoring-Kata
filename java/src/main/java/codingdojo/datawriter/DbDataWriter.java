package codingdojo.datawriter;

import codingdojo.domain.Customer;
import codingdojo.service.CustomerDataAccess;
import java.util.Collection;

public class DbDataWriter implements DataWriter {

  public final CustomerDataAccess customerDataAccess;

  public DbDataWriter(CustomerDataAccess customerDataAccess) {
    this.customerDataAccess = customerDataAccess;
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
        .forEach(this.customerDataAccess::updateCustomerRecord);

      duplicates.stream()
        .filter(Customer::isNotInternal)
        .forEach(c -> this.customerDataAccess.createCustomerRecord(c));
  }

  private ACTION updateCustomer(Customer customer) {
    this.customerDataAccess.updateCustomerRecord(customer);
    this.customerDataAccess.updateShoppingList(customer, customer.shoppingLists());
    return ACTION.UPDATE;
  }

  private ACTION createCustomerRecord(Customer newCustomer) {
    this.customerDataAccess.createCustomerRecord(newCustomer);
    return ACTION.CREATE;
  }
}
