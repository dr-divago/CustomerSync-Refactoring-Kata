package codingdojo;

import codingdojo.domain.Address;

public class AddressPrinter {
    public static String printAddress(Address address) {
        if (address == null) {
            return "'null'";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\'");
        sb.append(address.street());
        sb.append(", ");
        sb.append(address.postalCode());
        sb.append(" ");
        sb.append(address.city());
        sb.append("\'");
        return sb.toString();
    }
}
