package codingdojo;

import java.util.*;

/**
 * Fake implementation of data layer that stores data in-memory
 */
public class FakeDatabase implements CustomerDataLayer {

    private final HashMap<String, Customer> customersByExternalId = new HashMap<String, Customer>();
    private final HashMap<String, Customer> customersByMasterExternalId = new HashMap<String, Customer>();
    private final HashMap<String, Customer> customersByCompanyNumber = new HashMap<String, Customer>();
    private final Set<ShoppingList> shoppingLists = new HashSet<>();


    public void addCustomer(Customer customer) {
        if (customer.externalId().isPresent()) {
            this.customersByExternalId.put(customer.externalId().get(), customer);
        }
        if (customer.masterExternalId().isPresent()) {
            this.customersByMasterExternalId.put(customer.masterExternalId().get(), customer);
        }
        if (customer.companyNumber().isPresent()) {
            this.customersByCompanyNumber.put(customer.companyNumber().get(), customer);
        }
        if (!customer.shoppingLists().isEmpty()) {
            shoppingLists.addAll(customer.shoppingLists());
        }
    }

    @Override
    public Customer updateCustomerRecord(Customer customer) {
        this.addCustomer(customer);
        return customer;
    }

    @Override
    public Customer createCustomerRecord(Customer customer) {
        Customer newCustomer = ImmutableCustomer.copyOf(customer).withInternalId("fake internalId");
        addCustomer(newCustomer);
        return newCustomer;
    }

    @Override
    public void updateShoppingList(ShoppingList consumerShoppingList) {
        shoppingLists.add(consumerShoppingList);
    }

    @Override
    public Optional<Customer> findByExternalId(String externalId) {
        return Optional.ofNullable(this.customersByExternalId.get(externalId));
    }

    @Override
    public Optional<Customer> findByMasterExternalId(String masterExternalId) {
        return Optional.ofNullable(this.customersByMasterExternalId.get(masterExternalId));
    }

    @Override
    public Optional<Customer> findByCompanyNumber(String companyNumber) {
        return Optional.ofNullable(this.customersByCompanyNumber.get(companyNumber));
    }

    public List<Customer> getAllCustomers() {
        Set<Customer> allCustomers = new HashSet<Customer>(customersByExternalId.values());
        allCustomers.addAll(customersByMasterExternalId.values());
        allCustomers.addAll(customersByCompanyNumber.values());
        ArrayList<Customer> sortedList = new ArrayList<Customer>(allCustomers);
        sortedList.sort((o1, o2) -> Comparator.comparing(customer -> ((Customer)customer).internalId().map(String::toString).orElse(""))
                .thenComparing(customer -> ((Customer)customer).name().orElse(""))
                .compare(o1, o2));
        return sortedList;
    }

    public String printContents() {
        StringBuilder sb = new StringBuilder();
        sb.append("Fake Database.\nAll Customers {\n");
        for (Customer customer : getAllCustomers()) {
            sb.append(CustomerPrinter.print(customer, "    "));
            sb.append("\n");
        }

        sb.append("\n}");
        sb.append("\nAll Shopping Lists\n");
        List<ShoppingList> sortedShoppingLists = new ArrayList<>(shoppingLists);
        sortedShoppingLists.sort(Comparator.comparing(o -> o.getProducts().toString()));
        sb.append(ShoppingListPrinter.printShoppingLists(sortedShoppingLists, ""));
        return sb.toString();
    }
}
