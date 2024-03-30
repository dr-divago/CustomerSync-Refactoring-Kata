package codingdojo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class CustomerSync {

    private final CustomerDataAccess customerDataAccess;

    public CustomerSync(CustomerDataLayer customerDataLayer) {
        this(new CustomerDataAccess(customerDataLayer));
    }

    public CustomerSync(CustomerDataAccess db) {
        this.customerDataAccess = db;
    }

    public boolean syncWithDataLayer(ExternalCustomer externalCustomer) {

        CustomerMatches customerMatches = externalCustomer.isCompany() ? loadCompany(externalCustomer) : loadPerson(externalCustomer);

        Customer customer = customerMatches.customer()
          .orElse(ImmutableCustomer.builder()
            .externalId(externalCustomer.externalId())
            .masterExternalId(externalCustomer.externalId())
            .customerType(externalCustomer.isCompany() ? CustomerType.COMPANY : CustomerType.PERSON)
            .build());


        Customer newCustomer = populateFields(externalCustomer, customer);
        newCustomer = updateRelations(externalCustomer, newCustomer);
        Collection<Customer> duplicates = customerMatches.duplicates();
        if (!duplicates.isEmpty()) {
          for (Customer duplicate : customerMatches.duplicates()) {
            updateDuplicate(externalCustomer, duplicate);
          }
        }
        boolean created = false;
        if (!newCustomer.isInternal()) {
            newCustomer = createCustomer(newCustomer);
            created = true;
        } else {
            newCustomer = updateCustomer(newCustomer);
        }



        updateCustomer(newCustomer);

        return created;
    }

    private Customer updateRelations(ExternalCustomer externalCustomer, Customer customer) {
        List<ShoppingList> consumerShoppingLists = externalCustomer.shoppingLists();
        for (ShoppingList consumerShoppingList : consumerShoppingLists) {
          List<ShoppingList> newList = new ArrayList<>(customer.shoppingLists());
          newList.add(consumerShoppingList);
          Customer updateCustomer = ImmutableCustomer.copyOf(customer).withShoppingLists(newList);
          customer = this.customerDataAccess.updateShoppingList(updateCustomer, consumerShoppingList);
        }
        return customer;
    }

    private Customer updateCustomer(Customer customer) {
        return this.customerDataAccess.updateCustomerRecord(customer);
    }

    private Customer updateDuplicate(ExternalCustomer externalCustomer, Customer duplicate) {
      Customer newCustomer = ImmutableCustomer.copyOf(duplicate)
          .withName(externalCustomer.name());

        return newCustomer.isInternal() ? updateCustomer(newCustomer) : createCustomer(newCustomer);

    }

    private Customer createCustomer(Customer customer) {
        return this.customerDataAccess.createCustomerRecord(customer);
    }

    private Customer populateFields(ExternalCustomer externalCustomer, Customer customer) {
      return ImmutableCustomer.copyOf(customer)
        .withName(externalCustomer.name())
        .withCompanyNumber(externalCustomer.companyNumber())
        .withCustomerType(externalCustomer.isCompany() ? CustomerType.COMPANY : CustomerType.PERSON)
        .withAddress(externalCustomer.address())
        .withPreferredStore(externalCustomer.preferredStore());
    }

    public CustomerMatches loadCompany(ExternalCustomer externalCustomer) {
        return customerDataAccess.loadCompanyCustomer(externalCustomer.externalId(), externalCustomer.companyNumber().orElseThrow(IllegalStateException::new));
    }


  public CustomerMatches loadPerson(ExternalCustomer externalCustomer) {
        final String externalId = externalCustomer.externalId();

        CustomerMatches customerMatches = customerDataAccess.loadPersonCustomer(externalId);

        if (customerMatches.customer().isPresent()) {
            if (!CustomerType.PERSON.equals(customerMatches.customer().get().customerType())) {
                throw new ConflictException("Existing customer for externalCustomer " + externalId + " already exists and is not a person");
            }
        }

        return customerMatches;
    }


}
