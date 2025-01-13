public class User {
    private final String email;
    private final String customerId;
    private boolean isAdmin;

    public User(String email, String customerId, boolean isAdmin) {
        this.email = email;
        this.customerId = customerId;
        this.isAdmin = isAdmin;
    }

    public String getEmail() {
        return this.email;
    }
    public String getCustomerId() {
        return this.customerId;
    }

    public boolean isAdmin() {
        return this.isAdmin;
    }
}
