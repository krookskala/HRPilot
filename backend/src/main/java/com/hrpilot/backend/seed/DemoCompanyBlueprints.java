package com.hrpilot.backend.seed;

import com.hrpilot.backend.user.Role;

import java.util.List;

final class DemoCompanyBlueprints {

    private DemoCompanyBlueprints() {
    }

    static List<DepartmentBlueprint> departments() {
        return List.of(
            new DepartmentBlueprint(
                "engineering", "Engineering", null, Role.DEPARTMENT_MANAGER, 3,
                "Marco", "Weber",
                new PositionProfile("Director of Engineering", 128000, 142000),
                List.of(
                    new PositionProfile("Principal Engineer", 118000, 132000),
                    new PositionProfile("Engineering Operations Manager", 93000, 108000)
                )
            ),
            new DepartmentBlueprint(
                "platform", "Platform Engineering", "engineering", Role.DEPARTMENT_MANAGER, 14,
                "Stefan", "Krueger",
                new PositionProfile("Engineering Manager", 112000, 126000),
                List.of(
                    new PositionProfile("Staff Platform Engineer", 103000, 118000),
                    new PositionProfile("Senior Platform Engineer", 90000, 104000),
                    new PositionProfile("Senior Platform Engineer", 90000, 104000),
                    new PositionProfile("Platform Engineer", 76000, 89000),
                    new PositionProfile("Platform Engineer", 76000, 89000),
                    new PositionProfile("Site Reliability Engineer", 82000, 96000),
                    new PositionProfile("QA Automation Engineer", 72000, 85000)
                )
            ),
            new DepartmentBlueprint(
                "product-eng", "Product Engineering", "engineering", Role.DEPARTMENT_MANAGER, 14,
                "Nina", "Keller",
                new PositionProfile("Engineering Manager", 110000, 124000),
                List.of(
                    new PositionProfile("Staff Backend Engineer", 102000, 116000),
                    new PositionProfile("Senior Backend Engineer", 88000, 101000),
                    new PositionProfile("Senior Frontend Engineer", 86000, 99000),
                    new PositionProfile("Senior Frontend Engineer", 86000, 99000),
                    new PositionProfile("Full Stack Engineer", 76000, 90000),
                    new PositionProfile("Backend Engineer", 74000, 88000),
                    new PositionProfile("Frontend Engineer", 72000, 86000),
                    new PositionProfile("Mobile Engineer", 76000, 90000),
                    new PositionProfile("QA Engineer", 68000, 82000)
                )
            ),
            new DepartmentBlueprint(
                "data-ai", "Data & AI", "engineering", Role.DEPARTMENT_MANAGER, 7,
                "Tobias", "Hartmann",
                new PositionProfile("Head of Data & AI", 116000, 132000),
                List.of(
                    new PositionProfile("Senior Data Engineer", 90000, 106000),
                    new PositionProfile("Senior Machine Learning Engineer", 92000, 108000),
                    new PositionProfile("Machine Learning Engineer", 80000, 94000),
                    new PositionProfile("Analytics Engineer", 76000, 90000),
                    new PositionProfile("BI Analyst", 66000, 79000)
                )
            ),
            new DepartmentBlueprint(
                "it-ops", "IT Operations", "engineering", Role.DEPARTMENT_MANAGER, 5,
                "Jonas", "Beck",
                new PositionProfile("IT Operations Manager", 88000, 102000),
                List.of(
                    new PositionProfile("Security Engineer", 82000, 96000),
                    new PositionProfile("Systems Administrator", 62000, 76000),
                    new PositionProfile("Network Engineer", 70000, 84000),
                    new PositionProfile("IT Support Specialist", 46000, 58000)
                )
            ),
            new DepartmentBlueprint(
                "product-design", "Product & Design", null, Role.DEPARTMENT_MANAGER, 8,
                "Laura", "Neumann",
                new PositionProfile("Director of Product & Design", 112000, 128000),
                List.of(
                    new PositionProfile("Senior Product Manager", 90000, 104000),
                    new PositionProfile("Product Manager", 76000, 90000),
                    new PositionProfile("Senior Product Designer", 82000, 96000),
                    new PositionProfile("Product Designer", 68000, 82000),
                    new PositionProfile("UX Researcher", 70000, 84000),
                    new PositionProfile("Product Operations Analyst", 64000, 76000)
                )
            ),
            new DepartmentBlueprint(
                "sales", "Sales", null, Role.DEPARTMENT_MANAGER, 12,
                "Daniel", "Vogt",
                new PositionProfile("Sales Director", 118000, 136000),
                List.of(
                    new PositionProfile("Senior Account Executive", 84000, 98000),
                    new PositionProfile("Senior Account Executive", 84000, 98000),
                    new PositionProfile("Account Executive", 62000, 78000),
                    new PositionProfile("Account Executive", 62000, 78000),
                    new PositionProfile("Sales Development Representative", 46000, 58000),
                    new PositionProfile("Key Account Manager", 86000, 102000),
                    new PositionProfile("Revenue Operations Analyst", 62000, 76000)
                )
            ),
            new DepartmentBlueprint(
                "marketing", "Marketing", null, Role.DEPARTMENT_MANAGER, 7,
                "Sophie", "Richter",
                new PositionProfile("Marketing Director", 100000, 116000),
                List.of(
                    new PositionProfile("Growth Marketing Manager", 72000, 86000),
                    new PositionProfile("Performance Marketing Manager", 72000, 86000),
                    new PositionProfile("Content Strategist", 58000, 72000),
                    new PositionProfile("Brand Designer", 60000, 74000),
                    new PositionProfile("Marketing Operations Specialist", 56000, 68000),
                    new PositionProfile("Field Marketing Specialist", 54000, 66000)
                )
            ),
            new DepartmentBlueprint(
                "people-ops", "People Operations", null, Role.HR_MANAGER, 6,
                "Lena", "Hoffmann",
                new PositionProfile("HR Director", 98000, 114000),
                List.of(
                    new PositionProfile("HR Business Partner", 66000, 82000),
                    new PositionProfile("Talent Acquisition Partner", 62000, 78000),
                    new PositionProfile("People Operations Specialist", 54000, 66000),
                    new PositionProfile("Payroll Specialist", 56000, 70000),
                    new PositionProfile("HR Coordinator", 46000, 56000)
                )
            ),
            new DepartmentBlueprint(
                "finance", "Finance", null, Role.DEPARTMENT_MANAGER, 6,
                "Katharina", "Braun",
                new PositionProfile("Finance Director", 104000, 120000),
                List.of(
                    new PositionProfile("Controller", 90000, 106000),
                    new PositionProfile("Senior Accountant", 66000, 80000),
                    new PositionProfile("Financial Analyst", 64000, 78000),
                    new PositionProfile("Accounts Payable Specialist", 48000, 60000),
                    new PositionProfile("Procurement Analyst", 58000, 72000)
                )
            ),
            new DepartmentBlueprint(
                "customer-success", "Customer Success", null, Role.DEPARTMENT_MANAGER, 8,
                "Miriam", "Wolf",
                new PositionProfile("Director of Customer Success", 90000, 106000),
                List.of(
                    new PositionProfile("Senior Customer Success Manager", 72000, 86000),
                    new PositionProfile("Customer Success Manager", 58000, 74000),
                    new PositionProfile("Customer Success Manager", 58000, 74000),
                    new PositionProfile("Implementation Specialist", 56000, 70000),
                    new PositionProfile("Support Engineer", 54000, 68000),
                    new PositionProfile("Onboarding Specialist", 52000, 66000)
                )
            ),
            new DepartmentBlueprint(
                "operations", "Operations", null, Role.DEPARTMENT_MANAGER, 10,
                "Felix", "Werner",
                new PositionProfile("Operations Director", 94000, 110000),
                List.of(
                    new PositionProfile("Business Operations Analyst", 62000, 76000),
                    new PositionProfile("Compliance Specialist", 62000, 76000),
                    new PositionProfile("Office Manager", 50000, 64000),
                    new PositionProfile("Workplace Coordinator", 42000, 54000),
                    new PositionProfile("Procurement Specialist", 52000, 66000),
                    new PositionProfile("Administrative Specialist", 46000, 58000)
                )
            )
        );
    }
}
