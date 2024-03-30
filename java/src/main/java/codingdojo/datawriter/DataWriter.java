package codingdojo.datawriter;

import codingdojo.Customer;
import java.util.Collection;

public interface DataWriter {
  enum ACTION {
    CREATE,
    UPDATE
  }
  ACTION write(Customer customer);
  void write(Collection<Customer> duplicates);
}
