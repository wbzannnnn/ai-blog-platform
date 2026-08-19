package com.lanou.springaidemo.dto.response;

import com.lanou.springaidemo.entity.Posts;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {
    private Long id;
    private String title;
    private String content;
    private String summary;
    private UserResponse author;
    private List<TagResponse> tags;
    private Integer likeCount;
    private Integer viewCount;
    private Integer commentCount;
    private Long createdAt;
    
    /**
     * 从实体转换为响应DTO
     */
    public static PostResponse fromEntity(Posts post, UserResponse author,
                                          List<TagResponse> tags, long commentCount) {
        PostResponse response = new PostResponse();
        response.id = post.getId();
        response.title = post.getTitle();
        response.content = post.getContent();
        response.summary = post.getSummary();
        response.author = author;
        response.tags = tags;
        response.likeCount = post.getLikeCount();
        response.viewCount = post.getViewCount();
        response.commentCount = (int) commentCount;
        response.createdAt = post.getCreatedAt();
        return response;
    }
}