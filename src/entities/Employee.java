package entities;

public class Employee extends User {

    private String employeeId;
    private EmployeeRole role;

    // Constructor
    public Employee(int id, String firstName, String lastName, String username,
                    String email,String password,String employeeId, EmployeeRole role) {
        // Use super to call the User constructor
        super( id,  firstName, lastName, username, email, password);

        this.employeeId = employeeId;
        this.role = role;
    }

    public Employee(int id, String username, String password, String email, String role) {
        super(id, username, password, email);
        this.role = EmployeeRole.valueOf(role);
    }

    // Getters and Setters
    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

   /* public EmployeeRole getRole() {
        return role;
    }*/

    public void setRole(EmployeeRole role) {
        this.role = role;
    }
}