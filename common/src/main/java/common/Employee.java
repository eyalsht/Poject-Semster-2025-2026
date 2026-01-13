package common;

public class Employee extends User {

    private String employeeId;

    public Employee(int id, String firstName, String lastName, String username,
                    String email,String password, UserRole employeeRole,String employeeId) {

        super( id, username, password, firstName, lastName, email, employeeRole);
        this.employeeId = employeeId;
    }


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