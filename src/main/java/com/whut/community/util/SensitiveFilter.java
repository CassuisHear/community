package com.whut.community.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);

    // 替换符
    private static final String REPLACEMENT = "***";

    // 根节点
    private TrieNode rootNode = new TrieNode();

    // 使用 sensitive-words.txt 文件对 rootNode 成员进行初始化
    @PostConstruct
    public void init() {
        // 使用类加载器从 classes 目录中读取到敏感词文件
        try (
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is))
        ) {
            // 每次从 reader 中读取一行并添加到前缀树中
            String keyWord;
            while ((keyWord = reader.readLine()) != null) {
                this.addKeyWord(keyWord);
            }
        } catch (IOException e) {
            logger.error("加载敏感词文件失败 : " + e.getMessage());
        }
    }

    // 添加敏感词到前缀树中
    private void addKeyWord(String keyWord) {
        TrieNode curNode = this.rootNode;
        int n = keyWord.length();
        for (int i = 0; i < n; i++) {
            char ch = keyWord.charAt(i);

            // 尝试在当前节点上获取这个字符对应的子节点
            TrieNode subNode = curNode.getSubNode(ch);

            // 如果没有这个子节点，需要初始化
            if (subNode == null) {
                subNode = new TrieNode();
                curNode.addSubNode(ch, subNode);
            }

            // 如果是叶节点，这个节点的 isKeyWord 属性标记为 true
            if (i == n - 1) {
                subNode.setKeyWord(true);
            }

            // 当前节点移动到子节点上
            curNode = subNode;
        }
    }

    /**
     *  过滤敏感词的方法
     * @param text 可能含有敏感词的文本
     * @return 过滤之后的文本
     */
    public String filter(String text) {
        // 空值处理
        if (StringUtils.isBlank(text)) {
            return null;
        }

        int n = text.length();

        // 创建3个指针以及答案字符串
        TrieNode curNode = this.rootNode; // 指针1
        int begin = 0; // 指针2
        int position = 0; // 指针3
        StringBuilder ans = new StringBuilder(); // 最终答案

        while (begin < n) {
            /*
                为了避免出现以下情况，这里以 begin 的范围作为循环条件并对 position 的位置做出判断：
                敏感词：fabcd、abc
                需要检查的text的最后一段：fabc
                如果只是用 if (position < n) 内的逻辑处理，会漏掉最后一个敏感词
             */
            if (position < n) {
                char ch = text.charAt(position);

                // 需要跳过特殊字符
                if (isSpecialSymbol(ch)) {
                    // 当前特殊字符在起点时，指针2移动即可
                    if (curNode == this.rootNode) {
                        ans.append(ch);
                        begin++;
                    }
                    // 无论当前特殊字符在哪个位置，指针3都要向后移动
                    position++;
                    continue;
                }

                // 如果不是特殊字符，需要沿着前缀树检查下级节点
                curNode = curNode.getSubNode(ch);
                if (curNode == null) { // 区间[begin, position]内的字符串不构成敏感词
                    // begin 处的字符加入到答案中
                    ans.append(text.charAt(begin));
                    // 检查下一个位置
                    position = ++begin;
                    // curNode 归为根节点
                    curNode = this.rootNode;
                } else if(curNode.isKeyWord()) { // 区间[begin, position]内的字符串构成敏感词
                    // 替换掉敏感词：[begin, position]内的字符串
                    ans.append(REPLACEMENT);
                    // 检查下一位置
                    begin = ++position;
                    // curNode 归为根节点
                    curNode = this.rootNode;
                } else { // 区间[begin, position]内的字符串构成敏感词 的前缀但不构成整个敏感词
                    // 检查下一个字符
                    position++;
                }
            } else {
                ans.append(text.charAt(begin));
                position = ++begin;
                curNode = this.rootNode;
            }
        }

        return ans.toString();
    }

    /*
        判断是否为特殊字符：
        1.isAsciiAlphanumeric(ch) 方法判断 ch 是否是普通字符
        2.0x2E80~0x9FFF 是东南亚字符范围，超出这个范围认为是特殊字符
     */
    private boolean isSpecialSymbol(Character ch) {
        return !CharUtils.isAsciiAlphanumeric(ch) && (ch < 0x2E80 || ch > 0x9FFF);
    }

    // 成员内部类：前缀树
    private class TrieNode {
        // 是否为敏感词的 标记
        private boolean isKeyWord = false;

        // 子节点，key 是子节点的字符，value 是对应的节点结构
        Map<Character, TrieNode> subNodes = new HashMap<>();

        boolean isKeyWord() {
            return isKeyWord;
        }

        void setKeyWord(boolean keyWord) {
            isKeyWord = keyWord;
        }

        // 根据字符和节点添加一个子节点
        void addSubNode(Character ch, TrieNode node) {
            subNodes.put(ch, node);
        }

        // 根据字符获取子节点结构
        TrieNode getSubNode(Character ch) {
            return subNodes.get(ch);
        }
    }


}
