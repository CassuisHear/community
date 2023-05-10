package com.whut.community.controller;

import com.whut.community.service.AlphaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/alpha")
public class AlphaController {

    private final AlphaService alphaService;

    @Autowired
    public AlphaController(AlphaService alphaService) {
        this.alphaService = alphaService;
    }

    @RequestMapping("/hello")
    @ResponseBody
    public String sayHello() {
        return "Hello SpringBoot.";
    }

    @RequestMapping("/data")
    @ResponseBody
    public String getData() {
        return alphaService.serviceSelect();
    }

    @RequestMapping("/http")
    public void http(HttpServletRequest request, HttpServletResponse response) {

        //获取请求数据
        System.out.println(request.getMethod());
        System.out.println(request.getServletPath());
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            String value = request.getHeader(name);
            System.out.println(name + ": " + value);
        }
        System.out.println(request.getParameter("code"));

        //返回相应数据
        response.setContentType("text/html;charset=utf-8");
        try (PrintWriter writer = response.getWriter();) {
            writer.write("<h1>社区系统</h1>");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // /students?current=1&limit=20
    @RequestMapping(path = "/students", method = RequestMethod.GET)
    @ResponseBody
    public String getStudents(
            @RequestParam(name = "current", required = false, defaultValue = "1") int current,
            @RequestParam(name = "limit", required = false, defaultValue = "10") int limit) {

        System.out.println("current = " + current + " limit = " + limit);
        return "Here are some students...";
    }

    // /student/123
    @RequestMapping(path = "/student/{id}", method = RequestMethod.GET)
    @ResponseBody
    public String getStudent(
            @PathVariable(value = "id") int studentId) {
        System.out.println("studentId = " + studentId);
        return "Here is a student...";
    }

    //POST
    @RequestMapping(path = "/student", method = RequestMethod.POST)
    @ResponseBody
    public String saveStudent(String name, int age) {
        System.out.println("name = " + name);
        System.out.println("age = " + age);
        return "保存成功！";
    }

    //响应HTML页面
    @RequestMapping(path = "/teacher", method = RequestMethod.GET)
    public String getTeacher(Model model) {

        model.addAttribute("name", "张三");
        model.addAttribute("age", 30);

        return "/demo/teacher";
    }

    //响应JSON数据
    @RequestMapping(path = "/emp", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getEmp() {
        Map<String, Object> emp = new HashMap<>();

        emp.put("name", "张三");
        emp.put("age", 30);
        emp.put("salary", 8000.0);

        return emp;
    }

    //请求重定向
    @GetMapping("/test")
    public String test() {
        return "redirect:/html/test.html";
    }
}
