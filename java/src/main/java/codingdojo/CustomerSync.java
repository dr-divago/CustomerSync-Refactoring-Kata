package codingdojo;

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

        CustomerMatches customerMatches;
        if (externalCustomer.isCompany()) {
            customerMatches = loadCompany(externalCustomer);
        } else {
            customerMatches = loadPerson(externalCustomer);
        }
        Optional<Customer> maybeCustomer = customerMatches.customer();

        Customer customer;
        if (!maybeCustomer.isPresent()) {
            customer = ImmutableCustomer.builder()
                    .externalId(externalCustomer.externalId())
                    .masterExternalId(externalCustomer.externalId())
                    .customerType(externalCustomer.isCompany() ? CustomerType.COMPANY : CustomerType.PERSON)
                    .build();
        }
        else {
            customer = maybeCustomer.get();
        }

        Customer newCustomer = populateFields(externalCustomer, customer);

        boolean created = false;
        if (!newCustomer.isInternal()) {
            newCustomer = createCustomer(newCustomer);
            created = true;
        } else {
            newCustomer = updateCustomer(newCustomer);
        }
        newCustomer = updateContactInfo(externalCustomer, newCustomer);

        Collection<Customer> duplicates = customerMatches.duplicates();
        if (!duplicates.isEmpty()) {
            for (Customer duplicate : customerMatches.duplicates()) {
                duplicate = updateDuplicate(externalCustomer, duplicate);
            }
        }

        newCustomer = updateRelations(externalCustomer, newCustomer);
        newCustomer = updatePreferredStore(externalCustomer, newCustomer);
        updateCustomer(newCustomer);

        return created;
    }

    private Customer updateRelations(ExternalCustomer externalCustomer, Customer customer) {
        List<ShoppingList> consumerShoppingLists = externalCustomer.shoppingLists();
        for (ShoppingList consumerShoppingList : consumerShoppingLists) {
            customer = this.customerDataAccess.updateShoppingList(customer, consumerShoppingList);
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

    private Customer updatePreferredStore(ExternalCustomer externalCustomer, Customer customer) {
      return ImmutableCustomer.copyOf(customer).withPreferredStore(externalCustomer.preferredStore());
    }

    private Customer createCustomer(Customer customer) {
        return this.customerDataAccess.createCustomerRecord(customer);
    }

    private Customer populateFields(ExternalCustomer externalCustomer, Customer customer) {
      return ImmutableCustomer.copyOf(customer)
        .withName(externalCustomer.name())
        .withCompanyNumber(externalCustomer.companyNumber())
        .withCustomerType(externalCustomer.isCompany() ? CustomerType.COMPANY : CustomerType.PERSON);
    }

    private Customer updateContactInfo(ExternalCustomer externalCustomer, Customer customer) {
      return ImmutableCustomer.copyOf(customer)
          .withAddress(externalCustomer.address());
    }

    public CustomerMatches loadCompany(ExternalCustomer externalCustomer) {

        final String externalId = externalCustomer.externalId();
        final String companyNumber = externalCustomer.companyNumber().get();

        CustomerMatches customerMatches = customerDataAccess.loadCompanyCustomer(externalId, companyNumber);

        customerMatches.customer()
          .filter(c -> !CustomerType.COMPANY.equals(c.customerType()))
          .ifPresent(c -> { throw new ConflictException("Existing customer for externalCustomer " + externalId + " already exists and is not a company");});

        //customerMatches = customerMatches.matchedByExternalId() ? matchExternalId(companyNumber, customerMatches) : matchCompanyNumber(externalId, companyNumber, customerMatches);

        if (customerMatches.matchedByExternalId()) {
          customerMatches = matchDuplicate(companyNumber, customerMatches);
        } else if (customerMatches.matchedByCompanyNumber()) {
          customerMatches = matchCompanyNumber(externalId, companyNumber, customerMatches);
        }

        return customerMatches;
    }

  private static CustomerMatches matchCompanyNumber(final String externalId, final String companyNumber, CustomerMatches customerMatches) {
    String customerExternalId = customerMatches.customer().get().externalId().orElse("");
    if (!customerExternalId.isEmpty() && !externalId.equals(customerExternalId)) {
        throw new ConflictException("Existing customer for externalCustomer " + companyNumber + " doesn't match external id " + externalId + " instead found " + customerExternalId );
    }
    customerMatches = matchByCompany(externalId, customerMatches);
    return customerMatches;
  }

  private static CustomerMatches matchDuplicate(final String companyNumber, CustomerMatches customerMatches) {
    Optional<String> maybeCustomerCompanyNumber = customerMatches.customer().get().companyNumber();
    final CustomerMatches finalCustomerMatches = customerMatches;
    return maybeCustomerCompanyNumber
      .filter(customerCompanyNumber -> !companyNumber.equals(customerCompanyNumber))
      .map(c -> {
        Collection<Customer> duplicate = finalCustomerMatches.duplicates();
        duplicate.add(finalCustomerMatches.customer().get());
        return ImmutableCustomerMatches.builder()
          .from(finalCustomerMatches)
          .customer(Optional.empty())
          .matchTerm(Optional.empty())
          .duplicates(duplicate)
          .build();
      }).orElse(ImmutableCustomerMatches.copyOf(customerMatches));
  }

  public CustomerMatches loadPerson(ExternalCustomer externalCustomer) {
        final String externalId = externalCustomer.externalId();

        CustomerMatches customerMatches = customerDataAccess.loadPersonCustomer(externalId);

        if (customerMatches.customer().isPresent()) {
            if (!CustomerType.PERSON.equals(customerMatches.customer().get().customerType())) {
                throw new ConflictException("Existing customer for externalCustomer " + externalId + " already exists and is not a person");
            }

            if (!"ExternalId".equals(customerMatches.matchTerm().get())) {
              customerMatches = matchByCompany(externalId, customerMatches);
            }
        }

        return customerMatches;
    }

  private static CustomerMatches matchByCompany(final String externalId, CustomerMatches customerMatches) {
    Customer customer = ImmutableCustomer.copyOf(customerMatches.customer().get())
        .withExternalId(externalId)
        .withMasterExternalId(externalId);
    customerMatches = ImmutableCustomerMatches.copyOf(customerMatches).withCustomer(Optional.of(customer));
    return customerMatches;
  }
}
