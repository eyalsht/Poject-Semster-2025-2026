package common;

public class Employee extends User {

    private String employeeId;
   // private UserRole employeeRole;

    // Constructor
    public Employee(int id, String firstName, String lastName, String username,
                    String email,String password, UserRole employeeRole,String employeeId) {
        // Use super to call the User constructor
        super( id, username, password, firstName, lastName, email, employeeRole);
        this.employeeId = employeeId;
    }

   // public Employee(int id,  String firstName, String lastName,String username, String email, String password, UserRole employeeRole, String employeeId) {
       // super(id, firstName, lastName ,username, password, email, employeeRole);
       // this.role = UserRole.valueOf(employeeRole);
   // }

    // Getters and Setters
    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public UserRole getRole() {
        return this.role;
    }

    public void setRole(UserRole employeeRole) {
        this.role = employeeRole;
    }
}