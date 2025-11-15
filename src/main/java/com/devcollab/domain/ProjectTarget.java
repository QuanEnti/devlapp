package com.devcollab.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "[ProjectTarget]")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "target_id")
    private Long id;

    @Column(nullable = false)
    private int month;

    @Column(nullable = false)
    private int year;

    @Column(name = "target_count", nullable = false)
    private int targetCount;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;
}
