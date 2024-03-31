package codingdojo.dataloader;

import codingdojo.service.CustomerDataAccess;
import codingdojo.domain.CustomerMatches;
import codingdojo.domain.ExternalCustomer;

public class DBDataLoader implements DataLoader {

  private final CustomerDataAccess customerDataAccess;

  public DBDataLoader(CustomerDataAccess customerDataAccess) {
    this.customerDataAccess = customerDataAccess;
  }

  @Override
  public CustomerMatches load(ExternalCustomer externalCustomer) {
    return externalCustomer.isCompany()
      ? customerDataAccess.loadCompanyCustomer(externalCustomer.externalId(), externalCustomer.companyNumber().get())
      : customerDataAccess.loadPersonCustomer(externalCustomer.externalId());
  }
}
