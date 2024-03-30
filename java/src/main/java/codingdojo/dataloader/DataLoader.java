package codingdojo.dataloader;

import codingdojo.CustomerMatches;
import codingdojo.ExternalCustomer;

public interface DataLoader {
    CustomerMatches load(ExternalCustomer externalCustomer);
}
