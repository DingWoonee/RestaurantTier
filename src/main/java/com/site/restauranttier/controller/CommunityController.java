package com.site.restauranttier.controller;

import com.site.restauranttier.entity.Post;
import com.site.restauranttier.entity.PostComment;
import com.site.restauranttier.entity.User;
import com.site.restauranttier.repository.PostCommentRepository;
import com.site.restauranttier.repository.PostRepository;
import com.site.restauranttier.service.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class CommunityController {
    private final PostService postService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final PostCommentService postCommentService;
    private final PostRepository postRepository;
    private final PostCommentRepository postCommentRepository;
    private final PostScrapService postScrapService;

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    // 커뮤니티 메인 화면
    @GetMapping("/community")
    public String community(Model model, @RequestParam(defaultValue = "전체") String postCategory, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(defaultValue = "popular") String sort) {
        Page<Post> paging;
        // 따로 전송된 카테고리 값이 없을떄
        if (postCategory.equals("전체")) {
            paging = postService.getList(page, sort);
        } else {
            paging = postService.getListByPostCategory(postCategory, page,sort);
        }
        model.addAttribute("postCategory", postCategory);
        List<String> timeAgoList = postService.getTimeAgoList(paging);

        model.addAttribute("sort", sort);
        model.addAttribute("paging", paging);
        model.addAttribute("timeAgoList", timeAgoList);
        return "community";
    }

    // 커뮤니티 게시글 상세 화면
    @GetMapping("/community/{postId}")
    public String post(Model model, @PathVariable Integer postId, Principal principal, @RequestParam(defaultValue = "popular") String sort) {
        Post post = postService.getPost(postId);
        // 조회수 증가
        postService.increaseVisitCount(post);
        String timeAgoData = postService.timeAgo(LocalDateTime.now(), post.getCreatedAt());
        List<PostComment> postCommentList = new ArrayList<>();
        if (sort.equals("popular")) {
            postCommentList = post.getPostCommentList().stream().sorted(Comparator.comparing(PostComment::getLikeCount).reversed()).collect(Collectors.toList());
        } else if (sort.equals("recent")) {
            postCommentList = post.getPostCommentList().stream().sorted(Comparator.comparing(PostComment::getCreatedAt).reversed()).collect(Collectors.toList());
        }
        // Comment의 createdAt을 문자열로 변환하여 저장한 리스트
        List<String> commentCreatedAtList = postCommentService.getCreatedAtList(postCommentList);
        model.addAttribute("postCommentList", postCommentList);
        model.addAttribute("post", post);
        model.addAttribute("commentCreatedAtList", commentCreatedAtList);
        model.addAttribute("timeAgoData", timeAgoData);
        boolean isPostScrappedByUser = false;
        if (principal != null) {
            User user = customOAuth2UserService.getUser(principal.getName());
            model.addAttribute("user", user);
            isPostScrappedByUser = post.getPostScrapList().stream()
                    .anyMatch(scrap -> scrap.getUser().equals(user));
        }
        ;
        model.addAttribute("isPostScrappedByUser", isPostScrappedByUser);
        return "community_post";
    }

    // 커뮤니티 글 작성 화면
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    @GetMapping("/community/write")
    public String write() {
        return "community_write";
    }

    // 게시글 생성
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    @PostMapping("/api/community/post/create")
    public ResponseEntity<String> postCreate(
            @RequestParam("title") String title, @RequestParam("postCategory") String postCategory,
            @RequestParam("content") String content,
            Model model, Principal principal) {
        Post post = new Post(title, content, postCategory, "ACTIVE", LocalDateTime.now());
        User user = customOAuth2UserService.getUser(principal.getName());
        postService.create(post, user);
        return ResponseEntity.ok("글이 성공적으로 저장되었습니다.");
    }

    // 댓글 or 대댓글 생성
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    @PostMapping("/api/community/comment/create")
    public ResponseEntity<String> postCommentCreate(
            @RequestParam(name= "content", defaultValue = "") String content,
            @RequestParam(name = "postId") String postId,
            @RequestParam(name = "parentCommentId", defaultValue = "") String parentCommentId,
            Model model, Principal principal) {

        Integer postIdInt = Integer.valueOf(postId);
        User user = customOAuth2UserService.getUser(principal.getName());
        Post post = postService.getPost(postIdInt);
        PostComment postComment = new PostComment(content, "ACTIVE", LocalDateTime.now(), post, user);
        PostComment savedPostComment = postCommentRepository.save(postComment);

        // 대댓글이면 부모 관계 매핑하기
        if (!parentCommentId.isEmpty()) {
            PostComment parentComment = postCommentService.getPostCommentByCommentId(Integer.valueOf(parentCommentId));
            savedPostComment.setParentComment(parentComment);
            parentComment.getRepliesList().add(postComment);
            postCommentService.replyCreate(user,savedPostComment);
            postCommentRepository.save(parentComment);
        }
        // 댓글이면 post와 연결하면서 저장
        else{
            postCommentService.create(post, user, savedPostComment);
        }
        postCommentRepository.save(savedPostComment);
        return ResponseEntity.ok("댓글이 성공적으로 저장되었습니다.");
    }

    // 좋아요 생성
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    @GetMapping("/api/post/like")
    public ResponseEntity<String> postLikeCreate(@RequestParam("postId") String postId, Model model, Principal principal) {
        Integer postidInt = Integer.valueOf(postId);
        User user = customOAuth2UserService.getUser(principal.getName());
        Post post = postService.getPost(postidInt);
        postService.likeCreateOrDelete(post, user);
        return ResponseEntity.ok("게시글 좋아요가 처리 완료되었습니다");
    }
    // 싫어요 생성
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    @GetMapping("/api/post/dislike")
    public ResponseEntity<String> postDislikeCreate(@RequestParam("postId") String postId, Model model, Principal principal) {
        Integer postidInt = Integer.valueOf(postId);
        User user = customOAuth2UserService.getUser(principal.getName());
        Post post = postService.getPost(postidInt);
        postService.dislikeCreateOrDelete(post, user);
        return ResponseEntity.ok("게시글 싫어요가 처리 완료되었습니다");
    }
    // 즐겨찾기
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    @GetMapping("/api/post/scrap")
    public ResponseEntity<String> postScrap(@RequestParam("postId") String postId, Model model, Principal principal) {
        Integer postidInt = Integer.valueOf(postId);
        User user = customOAuth2UserService.getUser(principal.getName());
        Post post = postService.getPost(postidInt);
        postScrapService.scrapCreateOfDelete(post, user);
        return ResponseEntity.ok("게시글 북마크가 처리 완료되었습니다");
    }

    // 글 검색 화면
    @GetMapping("/community/search")
    public String search(Model model, @RequestParam(value = "page", defaultValue = "0") int page, @RequestParam(value = "kw", defaultValue = "") String kw, @RequestParam(defaultValue = "recent") String sort, @RequestParam(defaultValue = "전체") String postCategory) {

        Page<Post> paging = this.postService.getList(page, sort, kw, postCategory);
        List<String> timeAgoList = postService.getTimeAgoList(paging);
        model.addAttribute("timeAgoList", timeAgoList);
        model.addAttribute("paging", paging);
        model.addAttribute("postSearchKw", kw);
        model.addAttribute("sort",sort);
        model.addAttribute("postCategory",postCategory);
        return "community";
    }

    // 게시글 댓글 좋아요
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    @GetMapping("/api/comment/like/{commentId}")
    public ResponseEntity<String> likeComment(@PathVariable String commentId, Model model, Principal principal) {
        Integer commentIdInt = Integer.valueOf(commentId);
        PostComment postComment = postCommentService.getPostCommentByCommentId(commentIdInt);
        User user = customOAuth2UserService.getUser(principal.getName());
        postCommentService.likeCreateOrDelete(postComment, user);
        return ResponseEntity.ok("댓글 좋아요가 처리 완료되었습니다");
    }

    // 게시글 댓글 싫어요
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    @GetMapping("/api/comment/dislike/{commentId}")
    public ResponseEntity<String> dislikeComment(@PathVariable String commentId, Model model, Principal principal) {
        Integer commentIdInt = Integer.valueOf(commentId);
        PostComment postComment = postCommentService.getPostCommentByCommentId(commentIdInt);
        User user = customOAuth2UserService.getUser(principal.getName());
        postCommentService.dislikeCreateOrDelete(postComment, user);
        return ResponseEntity.ok("댓글 싫어요가 처리 완료되었습니다");
    }
}