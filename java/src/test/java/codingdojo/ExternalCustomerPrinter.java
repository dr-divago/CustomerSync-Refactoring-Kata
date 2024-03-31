package codingdojo;

import codingdojo.domain.ExternalCustomer;

public class ExternalCustomerPrinter {

    public static String print(ExternalCustomer externalCustomer, String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append("ExternalCustomer {");
        sb.append("\n" + indent + "    externalId='" + externalCustomer.externalId() + '\'');
        sb.append("\n" + indent + "    companyNumber='" + externalCustomer.companyNumber() + '\'' );
        sb.append("\n" + indent + "    name='" + externalCustomer.name() + '\'' );
        sb.append("\n" + indent + "    preferredStore='" + externalCustomer.preferredStore() + '\'');
        sb.append("\n" + indent + "    bonusPoints='" + externalCustomer.bonusPoints() + '\'');
        sb.append("\n" + indent + "    address=" + AddressPrinter.printAddress(externalCustomer.address()));
        sb.append("\n" + indent + "    shoppingLists=" + ShoppingListPrinter.printShoppingLists(externalCustomer.shoppingLists(), indent + "    ") );
        sb.append("\n" + indent + "}");

        return sb.toString();
    }
}
