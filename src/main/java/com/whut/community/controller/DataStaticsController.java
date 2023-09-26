package com.whut.community.controller;

import com.whut.community.service.DataStaticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
public class DataStaticsController {

    private DataStaticsService dataStaticsService;

    @Autowired
    public DataStaticsController(DataStaticsService dataStaticsService) {
        this.dataStaticsService = dataStaticsService;
    }

    // 返回统计页面
    @RequestMapping(value = "/data", method = {RequestMethod.GET, RequestMethod.POST})
    public String getDataPage() {
        return "/site/admin/data";
    }

    // 统计网站 UV
    @PostMapping("/data/uv")
    public String getUV(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
                        @DateTimeFormat(pattern = "yyyy-MM-dd") Date end, Model model) {
        long uvResult = start.after(end) ? 0 :
                dataStaticsService.calculateUV(start, end); // 开始时间不晚于结束时间才回去查询
        model.addAttribute("uvResult", uvResult);
        model.addAttribute("uvStartDate", start);
        model.addAttribute("uvEndDate", end);

        return "forward:/data";
    }

    // 统计网站 DAU
    @PostMapping("/data/dau")
    public String getDAU(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
                         @DateTimeFormat(pattern = "yyyy-MM-dd") Date end, Model model) {
        long dauResult = start.after(end) ? 0 :
                dataStaticsService.calculateDAU(start, end); // 开始时间不晚于结束时间才回去查询
        model.addAttribute("dauResult", dauResult);
        model.addAttribute("dauStartDate", start);
        model.addAttribute("dauEndDate", end);

        return "forward:/data";
    }
}
