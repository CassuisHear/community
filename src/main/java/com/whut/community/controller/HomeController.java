package com.whut.community.controller;

import com.whut.community.entity.DiscussPost;
import com.whut.community.entity.Page;
import com.whut.community.entity.User;
import com.whut.community.service.DiscussPostService;
import com.whut.community.service.UserService;
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
public class HomeController {

    private final DiscussPostService discussPostService;
    private final UserService userService;

    @Autowired
    public HomeController(DiscussPostService discussPostService, UserService userService) {
        this.discussPostService = discussPostService;
        this.userService = userService;
    }

    //添加访问此项目的路径，自动重定向请求到访问 index 页面
    @GetMapping("/")
    public String goHome() {
        return "redirect:/index";
    }

    @GetMapping("/index")
    public String getIndexPage(Model model, Page page, @RequestParam(name = "current", required = false) Integer current) {
        /*
            前端已经传入了 current 和 limit 属性到 page 中，
            现在只用在后端这里给 rows 属性和 path 属性赋值即可
         */
        //查询所有的数据，所以帖子的 userId 属性传入0
        page.setRows(discussPostService.findDiscussPostRows(0));
        page.setPath("/index");
        if (current != null) {
            page.setCurrent(current);
        }

        //修改参数使得查询由静态变成动态的
        List<DiscussPost> discussPostList = discussPostService.findDiscussPosts(0, page.getOffset(), page.getLimit());

        //DiscussPost 中只有 userId，但是我们希望可以得到 userId 对应的所有信息，
        //所以在这里使用一个 Map 集合来存放一个帖子DiscussPost和一个用户User，
        //再用一个 List 存放所有的帖子和对应的用户
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if (discussPostList != null) {
            for (DiscussPost discussPost : discussPostList) {
                Map<String, Object> ans = new HashMap<>();

                //将帖子DiscussPost本身放进Map集合中
                ans.put("post", discussPost);

                //将帖子对应的用户User放进Map集合中
                User user = userService.findUserById(discussPost.getUserId());
                ans.put("user", user);

                //将 Map 集合添加到 List：discussPosts 中
                discussPosts.add(ans);
            }
        }

        //将 List 放入 model 对象中
        model.addAttribute("discussPosts", discussPosts);

        return "/index";
    }
}
