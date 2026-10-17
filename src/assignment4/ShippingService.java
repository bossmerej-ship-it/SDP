package assignment4;

public class ShippingService {
    private String deliveryCity = "none";

    public boolean schedule(String city) {
        if (city == null || !(city.strip().equalsIgnoreCase("Kyzylorda")
                || city.strip().equalsIgnoreCase("Almaty")
                || city.strip().equalsIgnoreCase("Astana"))) {
            System.out.println("Delivery is unavailable in this city.");
            return false;
        }
        if (!deliveryCity.equals("none")) {
            System.out.println("A delivery is already scheduled.");
            return false;
        }
        deliveryCity = city.strip();
        System.out.println("Delivery scheduled to " + deliveryCity);
        return true;
    }

    public void cancel() {
        deliveryCity = "none";
        System.out.println("Delivery cancelled.");
    }

    public String getCity() {
        return deliveryCity;
    }
}
