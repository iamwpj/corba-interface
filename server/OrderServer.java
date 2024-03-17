import OrderApp.*;
import org.omg.CosNaming.*;
import org.omg.CORBA.*;
import org.omg.PortableServer.*;
import org.omg.PortableServer.POA;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class OrderManager extends OrderPOA {
    private ORB orb;

    public void setORB(ORB orb_val) {
        orb = orb_val;
    }

    // Class shared variables
    static List<String> foodItems = Arrays.asList("pizza", "taco", "gyro", "soup");
    static ArrayList<Integer> sessionPins = new ArrayList<Integer>();
    static String[] orders = new String[9999];
    static String[] statuses = new String[9999];

    public String sayHello() {
        return "Hello!\n";
    }

    public void setPin(int pin) {
        if (!(sessionPins.contains(pin))) {
            sessionPins.add(pin);
        }
    }

    public String printMenu() {
        String result = "Menu:\n";
        for (String food : foodItems) {
            result += " * ";
            result += food;
            result += "\n";
        }
        return result;
    }

    public String placeOrder(String order, int pin) {

        if (foodItems.contains(order)) {
            orders[pin] = order;
            statuses[pin] = "Started";
            return "Order placed: " + order;
        } else {
            return "Please select an item from the menu.";
        }
    }

    public String statusOrder(int pin) {
        String result = orders[pin] + " - " + statuses[pin];
        return result;
    }

    public String listOrders() {
        int pinsLength = sessionPins.size();
        String result = "Orders (" + pinsLength + "):\n";
        for (int pin : sessionPins) {
            result += " * ";
            result += pin + ": ";
            result += orders[pin];
            result += ", " + statuses[pin];
            result += "\n";
        }
        return result;
    }

    public boolean assumeManager(int pin) {
        if (pin == 1234) {
            return true;
        } else {
            return false;
        }
    }
}

public class OrderServer {

    public static void main(String[] args) {
        try {
            // create and initialize the ORB
            ORB orb = ORB.init(args, null);

            // get reference to root POA & activate the POAManager
            POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            rootpoa.the_POAManager().activate();

            // create servant and register it with the ORB
            OrderManager OrderManager = new OrderManager();
            OrderManager.setORB(orb);

            // get object reference from the servant
            org.omg.CORBA.Object ref = rootpoa.servant_to_reference(OrderManager);
            Order href = OrderHelper.narrow(ref);

            // get the root naming context
            // NameService invokes the name service
            org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
            // Use NamingContextExt which is part of the Interoperable
            // Naming Service (INS) specification.
            NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

            // bind the Object Reference in Naming
            String name = "Order";
            NameComponent[] path = ncRef.to_name(name);
            ncRef.rebind(path, href);

            // wait for invocations from clients
            orb.run();
        } catch (Exception e) {
            System.err.println("ERROR: " + e);
            e.printStackTrace(System.out);
        }

    }
}
