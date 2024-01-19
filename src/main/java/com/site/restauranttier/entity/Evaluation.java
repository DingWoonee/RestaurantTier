package com.site.restauranttier.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Getter
@Entity
@Table(name="evaluations_tbl")
public class Evaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer evaluationId;

    private String evaluationComment;

    private Double evaluationScore;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "evaluation")
    private List<EvaluationItemScore> EvaluationItemScoreList = new ArrayList<>();
}
