package codingdojo.dataloader;

import codingdojo.dao.CustomerDataLayer;
import codingdojo.domain.CustomerMatches;
import codingdojo.domain.ExternalCustomer;

public interface DataLoader {
  CustomerMatches load(ExternalCustomer externalCustomer);


  static DataLoader from(final ExternalCustomer externalCustomer, final CustomerDataLayer customerDataLayer) {
    if (externalCustomer.isCompany()) {
      return new CompanyDataLoader(customerDataLayer);
    }
    if (externalCustomer.isPerson()) {
      return new PersonDataLoader(customerDataLayer);
    }
    throw new IllegalArgumentException("Unknown customer type");
  }
}
