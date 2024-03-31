package codingdojo.dataloader;

import codingdojo.domain.CustomerMatches;
import codingdojo.domain.ExternalCustomer;

public interface DataLoader {
    CustomerMatches load(ExternalCustomer externalCustomer);
}
