package com.hrpilot.backend.department;

import com.hrpilot.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name ="departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @OneToOne
    @JoinColumn(name = "manager_id")
    private User manager;

    @ManyToOne
    @JoinColumn(name ="parent_dept_id")
    private Department parentDepartment;
}