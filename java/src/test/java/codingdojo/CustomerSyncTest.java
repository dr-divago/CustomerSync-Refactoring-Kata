package codingdojo;

import org.approvaltests.Approvals;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class CustomerSyncTest {

    /**
     * The external record already exists in the customer db, so no need to create it.
     * There is new data in some fields, which is merged in.
     */
    @Test
    public void syncCompanyByExternalId(){
        String externalId = "12345";
        String companyNumber = "470813-8895";

        ExternalCustomer externalCustomer = createExternalCompany(externalId, companyNumber);

        Customer customer = createCustomerWithSameCompanyAs(externalCustomer,
          Optional.of(externalId), Optional.of(companyNumber), Collections.emptyList(), Optional.empty());

        FakeDatabase db = new FakeDatabase();
        db.addCustomer(customer);
        CustomerSync sut = new CustomerSync(db);

        StringBuilder toAssert = printBeforeState(externalCustomer, db);

        // ACT
        boolean created = sut.syncWithDataLayer(externalCustomer);

        assertFalse(created);
        printAfterState(db, toAssert);
        Approvals.verify(toAssert);
    }

    @Test
    public void syncPrivatePersonByExternalId(){
        String externalId = "12345";

        ExternalCustomer externalCustomer = createExternalPrivatePerson(externalId);

        Customer customer = ImmutableCustomer.builder()
                .externalId(externalId)
                .internalId("67576")
                .customerType(CustomerType.PERSON)
                .build();

        FakeDatabase db = new FakeDatabase();
        db.addCustomer(customer);
        CustomerSync sut = new CustomerSync(db);

        StringBuilder toAssert = printBeforeState(externalCustomer, db);

        // ACT
        boolean created = sut.syncWithDataLayer(externalCustomer);

        assertFalse(created);
        printAfterState(db, toAssert);
        Approvals.verify(toAssert);
    }

    @Test
    public void syncShoppingLists(){
        String externalId = "12345";
        String companyNumber = "470813-8895";

        ExternalCustomer externalCustomer = createExternalCompany(externalId, companyNumber);

        ShoppingList shoppingList = new ShoppingList("eyeliner", "blusher");
        Customer customer = createCustomerWithSameCompanyAs(externalCustomer,
          Optional.of(externalId),
          Optional.of(companyNumber),
          Arrays.asList(shoppingList),
          Optional.empty());

        FakeDatabase db = new FakeDatabase();
        db.addCustomer(customer);
        CustomerSync sut = new CustomerSync(db);

        StringBuilder toAssert = printBeforeState(externalCustomer, db);

        // ACT
        boolean created = sut.syncWithDataLayer(externalCustomer);

        assertFalse(created);
        printAfterState(db, toAssert);
        Approvals.verify(toAssert);
    }

    @Test
    public void syncNewCompanyCustomer(){
        String externalId = "12345";
        String companyNumber = "470813-8895";
        ExternalCustomer externalCustomer = createExternalCompany(externalId, companyNumber);

        FakeDatabase db = new FakeDatabase();
        CustomerSync sut = new CustomerSync(db);

        StringBuilder toAssert = printBeforeState(externalCustomer, db);

        // ACT
        boolean created = sut.syncWithDataLayer(externalCustomer);

        assertTrue(created);
        printAfterState(db, toAssert);
        Approvals.verify(toAssert);
    }

    @Test
    public void syncNewPrivateCustomer(){
        String externalId = "12345";
        ExternalCustomer externalCustomer = createExternalPrivatePerson(externalId);

        FakeDatabase db = new FakeDatabase();
        CustomerSync sut = new CustomerSync(db);

        StringBuilder toAssert = printBeforeState(externalCustomer, db);

        // ACT
        boolean created = sut.syncWithDataLayer(externalCustomer);

        assertTrue(created);
        printAfterState(db, toAssert);
        Approvals.verify(toAssert);
    }

    @Test
    public void conflictExceptionWhenExistingCustomerIsPerson() {
        String externalId = "12345";
        String companyNumber = "470813-8895";

        ExternalCustomer externalCustomer = createExternalCompany(externalId,companyNumber);

        Customer customer = ImmutableCustomer.builder()
                .externalId(externalId)
                .internalId("45435")
                .customerType(CustomerType.PERSON)
                .build();

        FakeDatabase db = new FakeDatabase();
        db.addCustomer(customer);
        CustomerSync sut = new CustomerSync(db);

        StringBuilder toAssert = printBeforeState(externalCustomer, db);

        Assertions.assertThrows(ConflictException.class, () -> {
            sut.syncWithDataLayer(externalCustomer);
        }, printAfterState(db, toAssert).toString());

        Approvals.verify(toAssert);
    }

    @Test
    public void syncByExternalIdButCompanyNumbersConflict(){
        String externalId = "12345";
        String companyNumber = "470813-8895";

        ExternalCustomer externalCustomer = createExternalCompany(externalId, companyNumber);

        Customer customer = createCustomerWithSameCompanyAs(externalCustomer,
          Optional.of(externalId),
          Optional.of("000-3234"),
          Collections.emptyList(), Optional.empty());

        FakeDatabase db = new FakeDatabase();
        db.addCustomer(customer);
        CustomerSync sut = new CustomerSync(db);

        StringBuilder toAssert = printBeforeState(externalCustomer, db);

        // ACT
        boolean created = sut.syncWithDataLayer(externalCustomer);

        //assertTrue(created);
        printAfterState(db, toAssert);
        Approvals.verify(toAssert);
    }


    @Test
    public void syncByCompanyNumber(){
        String companyNumber = "12345";

        ExternalCustomer externalCustomer = createExternalCompany("12345", companyNumber);

        Customer customer = createCustomerWithSameCompanyAs(externalCustomer,
          Optional.empty(),
          Optional.of(companyNumber),
          Arrays.asList(new ShoppingList("eyeliner", "mascara", "blue bombe eyeshadow")),
          Optional.empty());

        FakeDatabase db = new FakeDatabase();
        db.addCustomer(customer);
        CustomerSync sut = new CustomerSync(db);

        StringBuilder toAssert = printBeforeState(externalCustomer, db);

        // ACT
        boolean created = sut.syncWithDataLayer(externalCustomer);

        assertFalse(created);
        printAfterState(db, toAssert);
        Approvals.verify(toAssert);
    }

    @Test
    public void syncByCompanyNumberWithConflictingExternalId(){
        String companyNumber = "12345";
        String externalId = "45646";

        ExternalCustomer externalCustomer = createExternalCompany(externalId, companyNumber);

        Customer customer = createCustomerWithSameCompanyAs(externalCustomer,
          Optional.of("conflicting id"),
          Optional.of(companyNumber),
          Collections.emptyList(),
          Optional.empty());

        FakeDatabase db = new FakeDatabase();
        db.addCustomer(customer);
        CustomerSync sut = new CustomerSync(db);

        StringBuilder toAssert = printBeforeState(externalCustomer, db);

        // ACT
        Assertions.assertThrows(ConflictException.class, () -> {
            sut.syncWithDataLayer(externalCustomer);
        }, printAfterState(db, toAssert).toString());

        Approvals.verify(toAssert);
    }

    @Test
    public void conflictExceptionWhenExistingCustomerIsCompany() {
        String externalId = "12345";

        ExternalCustomer externalCustomer = createExternalPrivatePerson(externalId);

        Customer customer = ImmutableCustomer.builder()
          .externalId(externalId)
          .internalId("45435")
          .companyNumber("32423-342")
          .customerType(CustomerType.COMPANY)
          .build();

        FakeDatabase db = new FakeDatabase();
        db.addCustomer(customer);
        CustomerSync sut = new CustomerSync(db);

        StringBuilder toAssert = printBeforeState(externalCustomer, db);

        Assertions.assertThrows(ConflictException.class, () -> {
            sut.syncWithDataLayer(externalCustomer);
        }, printAfterState(db, toAssert).toString());

        Approvals.verify(toAssert);
    }

    @Test
    public void syncCompanyByExternalIdWithNonMatchingMasterId(){
        String externalId = "12345";
        String companyNumber = "470813-8895";

        ExternalCustomer externalCustomer = createExternalCompany(externalId, companyNumber);

        Customer customer = createCustomerWithSameCompanyAs(externalCustomer,
          Optional.of(externalId),
          Optional.of(companyNumber),
          Collections.emptyList(),
          Optional.of("company 1"));

        Customer customer2 = ImmutableCustomer.builder()
                .companyNumber(externalCustomer.companyNumber())
                .internalId("45435234")
                .customerType(CustomerType.COMPANY)
                .masterExternalId(externalId)
                .name("company 2")
                .build();

        FakeDatabase db = new FakeDatabase();
        db.addCustomer(customer);
        db.addCustomer(customer2);
        CustomerSync sut = new CustomerSync(db);

        StringBuilder toAssert = printBeforeState(externalCustomer, db);

        // ACT
        boolean created = sut.syncWithDataLayer(externalCustomer);

        assertFalse(created);
        printAfterState(db, toAssert);
        Approvals.verify(toAssert);
    }


    private ExternalCustomer createExternalPrivatePerson(String externalId) {
        return ImmutableExternalCustomer.builder()
                .externalId(externalId)
                .name("Joe Bloggs")
                .address(new Address("123 main st", "Stockholm", "SE-123 45"))
                .preferredStore("Nordstan")
                .addShoppingLists(new ShoppingList("lipstick", "foundation"))
                .build();
    }


    private ExternalCustomer createExternalCompany(String externalId, String companyNumber) {
        return ImmutableExternalCustomer.builder()
                .externalId(externalId)
                .name("Acme Inc.")
                .address(new Address("123 main st", "Helsingborg", "SE-123 45"))
                .companyNumber(companyNumber)
                .addShoppingLists(new ShoppingList("lipstick", "blusher"))
                .build();
    }

    private Customer createCustomerWithSameCompanyAs(ExternalCustomer externalCustomer,
                                                     Optional<String> externalId,
                                                     Optional<String> companyNumber,
                                                     List<ShoppingList> shoppingList,
                                                     Optional<String> name) {
      return ImmutableCustomer.builder()
        .externalId(externalId)
        .companyNumber(externalCustomer.companyNumber().orElse(""))
        .internalId("45435")
        .customerType(CustomerType.COMPANY)
        .companyNumber(companyNumber.orElse(""))
        .shoppingLists(shoppingList)
        .name(name)
        .build();
    }

    private StringBuilder printBeforeState(ExternalCustomer externalCustomer, FakeDatabase db) {
        StringBuilder toAssert = new StringBuilder();
        toAssert.append("BEFORE:\n");
        toAssert.append(db.printContents());

        toAssert.append("\nSYNCING THIS:\n");
        toAssert.append(ExternalCustomerPrinter.print(externalCustomer, ""));
        return toAssert;
    }

    private StringBuilder printAfterState(FakeDatabase db, StringBuilder toAssert) {
        toAssert.append("\nAFTER:\n");
        toAssert.append(db.printContents());
        return toAssert;
    }
}
