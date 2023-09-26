package com.whut.community.controller;

import com.whut.community.entity.DiscussPost;
import com.whut.community.entity.Page;
import com.whut.community.service.ElasticsearchService;
import com.whut.community.service.LikeService;
import com.whut.community.service.UserService;
import com.whut.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController implements CommunityConstant {

    private ElasticsearchService elasticService;

    private UserService userService;

    private LikeService likeService;

    @Autowired
    public SearchController(ElasticsearchService elasticService,
                            UserService userService,
                            LikeService likeService) {
        this.elasticService = elasticService;
        this.userService = userService;
        this.likeService = likeService;
    }

    // 请求格式： localhost:8080//community/search?keyWord=xxx
    @GetMapping("/search")
    public String search(String keyWord, Page page, Model model,
                         @RequestParam(name = "current", required = false) Integer current) {
        // 1.根据 关键词和分页信息搜索帖子，
        // 由于 Page 类的设计问题，程序运行到这个位置时，page.getCurrent()值恒为1
        current = current == null ? page.getCurrent() : current;
        var searchResult =
                elasticService.searchDiscussPost(keyWord, current - 1, page.getLimit());

        // 2.将 searchResult 转化为 VO 对象集合
        List<Map<String, Object>> discussPostVoList = new ArrayList<>();
        if (searchResult != null) {
            for (DiscussPost post : searchResult) {
                Map<String, Object> discussPostVo = new HashMap<>();

                // 2.1添加帖子本身
                discussPostVo.put("post", post);

                // 2.2添加帖子的作者
                discussPostVo.put("user", userService.findUserById(post.getUserId()));

                // 2.3添加帖子的点赞数量
                discussPostVo.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));

                discussPostVoList.add(discussPostVo);
            }
        }
        model.addAttribute("discussPostVoList", discussPostVoList);

        // 3.将 keyWord 添加到 model 中
        model.addAttribute("keyWord", keyWord);

        // 4.构建分页信息
        page.setPath("/search?keyWord=" + keyWord);
        page.setRows(searchResult == null ? 0 : (int) searchResult.getTotalElements());
        page.setCurrent(current);

        return "/site/search";
    }
}
