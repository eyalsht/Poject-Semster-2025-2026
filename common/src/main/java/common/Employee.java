package common;

public class Employee extends User {

    private String employeeId;
    private UserRole employeeRole;

    // Constructor
    public Employee(int id, String firstName, String lastName, String username,
                    String email,String password,String employeeId, UserRole employeeRole) {
        // Use super to call the User constructor
        super( id,  firstName, lastName, username, email, password);

        this.employeeId = employeeId;
        this.employeeRole = employeeRole;
    }

    public Employee(int id, String username, String password, String email, String employeeRole) {
        super(id, username, password, email);
        this.employeeRole = UserRole.valueOf(employeeRole);
    }

    // Getters and Setters
    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public UserRole getRole() {
        return this.employeeRole;
    }

    public void setRole(UserRole employeeRole) {
        this.employeeRole = employeeRole;
    }
}