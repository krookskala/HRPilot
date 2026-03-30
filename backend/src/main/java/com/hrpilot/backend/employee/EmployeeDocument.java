package com.hrpilot.backend.employee;

import com.hrpilot.backend.common.BaseEntity;
import com.hrpilot.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "employee_documents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_user_id")
    private User uploadedByUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private EmployeeDocumentType documentType;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "storage_key", nullable = false, unique = true)
    private String storageKey;

    @Column(name = "file_size", nullable = false)
    private long fileSize;
}
