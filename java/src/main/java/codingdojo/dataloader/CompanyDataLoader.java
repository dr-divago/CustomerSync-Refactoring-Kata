package codingdojo.dataloader;

import codingdojo.dao.CustomerDataLayer;
import codingdojo.domain.Customer;
import codingdojo.domain.CustomerMatches;
import codingdojo.domain.ExternalCustomer;
import codingdojo.domain.ImmutableCustomer;
import codingdojo.domain.ImmutableCustomerMatches;
import codingdojo.service.ConflictException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;

public class CompanyDataLoader implements DataLoader {

  private final CustomerDataLayer customerDataLayer;

  public CompanyDataLoader(CustomerDataLayer customerDataLayer) {
    this.customerDataLayer = customerDataLayer;
  }

  @Override
  public CustomerMatches load(ExternalCustomer externalCustomer) {
    Optional<Customer> maybeCustomerMatchedByExternalId = this.customerDataLayer.findByExternalId(
      externalCustomer.externalId());

    maybeCustomerMatchedByExternalId
        .filter(Customer::isNotCompany)
        .ifPresent(c -> {
          throw new ConflictException("Existing customer for externalCustomer " + externalCustomer.externalId()
            + " already exists and is not a company");
        });

    return maybeCustomerMatchedByExternalId
      .map(customer -> Pair.of(
        customer,
        findDuplicate(externalCustomer.externalId(), Collections.singletonList(customer))))
      .map(customerAndDuplicate -> buildCustomerMatchByExternalId(
        customerAndDuplicate.getLeft(),
        externalCustomer.companyNumber().get(),
        customerAndDuplicate.getRight()))
      .orElse(buildCustomerMatchByCompany(externalCustomer.externalId(), externalCustomer.companyNumber().get()));
  }

  private List<Customer> findDuplicate(
    final String externalId,
    final List<Customer> dup) {
    return
      findDuplicateCustomer(externalId)
        .map(duplicateCustomer -> mergeDuplicate(duplicateCustomer, dup))
        .orElse(dup);
  }

  private Optional<Customer> findDuplicateCustomer(final String externalId) {
    return this.customerDataLayer.findByMasterExternalId(externalId);
  }

  private static List<Customer> mergeDuplicate(
      final Customer matchByMasterId,
      final List<Customer> dup) {
    return Stream.concat(dup.stream(), Stream.of(matchByMasterId)).collect(Collectors.toList());
  }

  private CustomerMatches buildCustomerMatchByExternalId(
      final Customer matchByExternalId,
      final String companyNumber,
      final List<Customer> mergedDuplicate) {
    return matchByExternalId.companyNumber().get().equals(companyNumber)
      ? ImmutableCustomerMatches.of(Optional.of(matchByExternalId), mergedDuplicate)
      : ImmutableCustomerMatches.of(Optional.empty(), mergedDuplicate);
  }

  private CustomerMatches buildCustomerMatchByCompany(
      final String externalId,
      final String companyNumber) {
    return this.customerDataLayer.findByCompanyNumber(companyNumber)
      .map(customer -> matchCompanyNumber(externalId, companyNumber, customer))
      .orElse(ImmutableCustomerMatches.of(Optional.empty(), Collections.emptyList()));
  }

  private static CustomerMatches matchCompanyNumber(
      final String externalId,
      final String companyNumber,
      final Customer customerMatched) {

    customerMatched.externalId()
        .ifPresent(customerExternalId -> throwsIfConflict(
          externalId,
          companyNumber,
          customerExternalId));

    return ImmutableCustomerMatches.builder()
      .customer(ImmutableCustomer.copyOf(customerMatched)
        .withExternalId(externalId)
        .withMasterExternalId(externalId))
      .duplicates(Collections.emptyList())
      .build();
  }

  private static void throwsIfConflict(
      final String externalId,
      final String companyNumber,
      final String customerExternalId) {

    if (!externalId.equals(customerExternalId)) {
      throw new ConflictException(
        "Existing customer for externalCustomer " + companyNumber + " doesn't match external id "
          + externalId + " instead found " + customerExternalId);
    }
  }
}
