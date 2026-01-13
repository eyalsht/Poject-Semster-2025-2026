package common.user;

import common.enums.EmployeeRole;
import jakarta.persistence.*;

@Entity
@Table(name = "employees") // Creates a separate table for employees (besides the user's table)
@DiscriminatorValue("Employee")  // Default for employees
@PrimaryKeyJoinColumn(name = "id") // The link between the employee and the user table
public class Employee extends User {

    private static final long serialVersionUID = 1L;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_role")
    private EmployeeRole role;

    // JPA no-arg constructor
    public Employee() {
        super();
    }

    public Employee(int id, String firstName, String lastName, String username,
                    String email, String password, EmployeeRole role) {
        super(id, username, password, firstName, lastName, email);
        this.role = role;
    }

    // ==================== GETTERS & SETTERS ====================
    public EmployeeRole getRole() { return role; }
    public void setRole(EmployeeRole role) { this.role = role; }
}