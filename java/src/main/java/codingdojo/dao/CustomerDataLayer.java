package codingdojo.dao;

import codingdojo.domain.Customer;
import codingdojo.domain.ShoppingList;
import java.util.Optional;

public interface CustomerDataLayer {

    Customer updateCustomerRecord(Customer customer);

    Customer createCustomerRecord(Customer customer);

    void updateShoppingList(ShoppingList consumerShoppingList);

    Optional<Customer> findByExternalId(String externalId);

    Optional<Customer> findByMasterExternalId(String externalId);

    Optional<Customer> findByCompanyNumber(String companyNumber);
}
