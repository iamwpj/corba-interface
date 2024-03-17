import OrderApp.*;
import org.omg.CosNaming.*;
import org.omg.CosNaming.NamingContextPackage.*;
import org.omg.CORBA.*;
import java.util.Scanner;

public class OrderClient {

  static Order orderImpl;
  static String order;
  static int pin;

  public static int collect_input(String[] args) {

    Scanner reader = new Scanner(System.in);

    int pinLength = String.valueOf(pin).length();
    if (!(pinLength == 4)) {
      System.out.print("Set a 4 digit PIN for your session > ");
      pin = reader.nextInt();

      // verify
      pinLength = String.valueOf(pin).length();
      if (!(pinLength == 4)) {
        System.out.println("You had one chance to set a PIN. Good bye.");
        reader.close();
        return 0;
      }
    
      orderImpl.setPin(pin);
    }

    // prepend
    System.out.print("[" + pin + "]: ");

    System.out.print(
        "Select from available actions\n  1 - Show menu\n  2 - Place order\n  3 - Show queued order\n  4 - Manager Options\n  5 - Change session\n  0 - Quit\n  9 - Greeting\n> ");

    int selection = reader.nextInt();

    if (selection == 0) {
      System.out.println("Good bye!");
      reader.close();
      return selection;
    }

    if (selection == 2) {

      // blank the input
      reader.nextLine();

      System.out.print("\nPlease input your order > ");
      order = reader.nextLine();
    }

    if (selection == 4) {

      // Let's make sure we can be a manager
      if (orderImpl.assumeManager(pin)) {
        System.out.print("Choose a manager option:\n 6 - List orders\n> ");
        selection = reader.nextInt();
      } else {
        // Return to main menu.
        System.out.println("\n*** You're not a manager!\nConsider placing an order.\n---\n");
        selection = 1;
      }

    }

    return selection;
  }

  public static void main(String[] args) {
    try {
      // create and initialize the ORB
      ORB orb = ORB.init(args, null);

      // get the root naming context
      org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
      // Use NamingContextExt instead of NamingContext. This is
      // part of the Interoperable naming Service.
      NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
      orderImpl = OrderHelper.narrow(ncRef.resolve_str("Order"));
      int mainMenuSelection = collect_input(args);

      while (mainMenuSelection > 0) {

        switch (mainMenuSelection) {
          case 1:
            String resp_1 = orderImpl.printMenu();
            System.out.println(resp_1);
            mainMenuSelection = collect_input(args);
            continue;
          case 2:
            String resp_2 = "\n >>> " + orderImpl.placeOrder(order, pin) + "\n --- \n";
            System.out.println(resp_2);
            mainMenuSelection = collect_input(args);
            continue;
          case 3:
            String resp_3 = "\n >>> " + orderImpl.statusOrder(pin) + "\n --- \n";
            System.out.println(resp_3);
            mainMenuSelection = collect_input(args);
            continue;
          case 4: // reserved for manager elevation
          case 5:
            pin = 0;
            mainMenuSelection = collect_input(args);
            continue;
          case 6:
            String resp_6 = "\n >>> " + orderImpl.listOrders();
            System.out.println(resp_6);
            mainMenuSelection = collect_input(args);
            continue;
          case 9:
            System.out.println(orderImpl.sayHello());
            mainMenuSelection = collect_input(args);
            continue;

        }
      }

    } catch (Exception e) {
      System.out.println("ERROR : " + e);
      e.printStackTrace(System.out);
    }
  }

}