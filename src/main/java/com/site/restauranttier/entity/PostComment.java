package com.site.restauranttier.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Entity
@Table(name="post_comments_tbl")
public class PostComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer commentId;
    String commentBody;
    String status;
    @ManyToOne
    @JoinColumn(name="parent_comment_id")
    PostComment parentComment;

    @OneToMany(mappedBy = "parentComment")
    List<PostComment> repliesList = new ArrayList<>();
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    Integer likeCount=0;

    public PostComment(String commentBody, String status, LocalDateTime createdAt, Post post, User user) {
        this.commentBody = commentBody;
        this.status = status;
        this.createdAt = createdAt;
        this.post = post;
        this.user = user;
    }

    @ManyToOne
    @JoinColumn(name="post_id")
    Post post;
    @ManyToOne
    @JoinColumn(name="user_id")
    User user;

    public PostComment() {

    }

    @ManyToMany
    @JoinTable(name="comment_likes_tbl",joinColumns = @JoinColumn(name="comment_id"),inverseJoinColumns = @JoinColumn(name="user_id"))
    List<User> likeUserList = new ArrayList<>();
    @ManyToMany
    @JoinTable(name="comment_dislikes_tbl",joinColumns = @JoinColumn(name="comment_id"),inverseJoinColumns = @JoinColumn(name="user_id"))

    List<User> dislikeUserList = new ArrayList<>();

    public String calculateTimeAgo() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime past = this.getCreatedAt();

        // 연 차이 계산
        Long yearsDifference = ChronoUnit.YEARS.between(past, now);
        if (yearsDifference > 0) return yearsDifference.toString() + "년 전";

        // 월 차이 계산
        Long monthsDifference = ChronoUnit.MONTHS.between(past, now);
        if (monthsDifference > 0) return monthsDifference.toString() + "달 전";

        // 일 차이 계산
        Long daysDifference = ChronoUnit.DAYS.between(past, now);
        if (daysDifference > 0) return daysDifference.toString() + "일 전";

        // 시간 차이 계산
        Long hoursDifference = ChronoUnit.HOURS.between(past, now);
        if (hoursDifference > 0) return hoursDifference.toString() + "시간 전";

        // 분 차이 계산
        Long minutesDifference = ChronoUnit.MINUTES.between(past, now);
        if (minutesDifference > 0) return minutesDifference.toString() + "분 전";

        // 초 차이 계산
        Long secondsDifference = ChronoUnit.SECONDS.between(past, now);
        return secondsDifference.toString() + "초 전";
    }
}
