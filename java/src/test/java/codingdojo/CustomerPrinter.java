package codingdojo;

import codingdojo.domain.Customer;

public class CustomerPrinter {

    public static String print(Customer customer, String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n" + indent + "Customer {");
        sb.append("\n" + indent + "    externalId='" + customer.externalId() + '\'');
        sb.append("\n" + indent + "    masterExternalId='" + customer.masterExternalId() + '\'');
        sb.append("\n" + indent + "    companyNumber='" + customer.companyNumber() + '\'' );
        sb.append("\n" + indent + "    internalId='" + customer.internalId() + '\'' );
        sb.append("\n" + indent + "    name='" + customer.name() + '\'' );
        sb.append("\n" + indent + "    customerType=" + customer.customerType() );
        sb.append("\n" + indent + "    preferredStore='" + customer.preferredStore() + '\'');
        sb.append("\n" + indent + "    bonusPoints='" + customer.bonusPoints() + '\'');
        sb.append("\n" + indent + "    address=" + AddressPrinter.printAddress(customer.address().orElse(null)));
        sb.append("\n" + indent + "    shoppingLists=" + ShoppingListPrinter.printShoppingLists(customer.shoppingLists(), indent + "    ") );
        sb.append("\n" + indent + "}");
        return sb.toString();
    }

}
