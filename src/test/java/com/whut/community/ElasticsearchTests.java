package com.whut.community;

import com.whut.community.dao.DiscussPostMapper;
import com.whut.community.dao.elasticsearch.DiscussPostRepository;
import com.whut.community.entity.DiscussPost;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class ElasticsearchTests {

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    @Qualifier("discussPostRepository")
    private DiscussPostRepository discussRepository;

    @Autowired
    private ElasticsearchTemplate elasticTemplate;

    @Test
    public void testInsert() {
        discussRepository.save(discussPostMapper.selectDiscussPostById(241));
        discussRepository.save(discussPostMapper.selectDiscussPostById(242));
        discussRepository.save(discussPostMapper.selectDiscussPostById(243));
    }

    @Test
    public void testInsertList() {
        discussRepository.saveAll(discussPostMapper.selectDiscussPosts(101, 0, 100, 0));
        discussRepository.saveAll(discussPostMapper.selectDiscussPosts(102, 0, 100, 0));
        discussRepository.saveAll(discussPostMapper.selectDiscussPosts(103, 0, 100, 0));
        discussRepository.saveAll(discussPostMapper.selectDiscussPosts(111, 0, 100, 0));
        discussRepository.saveAll(discussPostMapper.selectDiscussPosts(112, 0, 100, 0));
        discussRepository.saveAll(discussPostMapper.selectDiscussPosts(131, 0, 100, 0));
        discussRepository.saveAll(discussPostMapper.selectDiscussPosts(132, 0, 100, 0));
        discussRepository.saveAll(discussPostMapper.selectDiscussPosts(133, 0, 100, 0));
        discussRepository.saveAll(discussPostMapper.selectDiscussPosts(134, 0, 100, 0));
    }

    @Test
    public void testUpdate() {
        DiscussPost post = discussPostMapper.selectDiscussPostById(231);
        post.setContent("我是新人，使劲灌水...");
        discussRepository.save(post);
    }

    @Test
    public void testDelete() {
        //discussRepository.deleteById(231);
        discussRepository.deleteAll();
    }

    @Test
    public void testSearchByRepository() {
        // 构造搜索条件
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content")) // 查询字段
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC)) // 3个排序条件，从上到下优先级依次降低
                .withPageable(PageRequest.of(0, 10)) // 分页条件
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>") // 高亮字段(实际上是添加标签)
                ).build();

        // elasticTemplate.queryForPage(searchQuery, class, SearchResultMapper)
        // 底层获取得到了高亮显示的值，但是没有返回

        // 查询结果是一个分页对象
        Page<DiscussPost> page = discussRepository.search(searchQuery);
        System.out.println(page.getTotalElements()); // 元素总数
        System.out.println(page.getTotalPages()); // 分页总数
        System.out.println(page.getNumber()); // 当前页面
        System.out.println(page.getSize()); // 每页大小
        for (DiscussPost post : page) {
            System.out.println(post);
        }
    }

    @Test
    public void testSearchByTemplate() {
        // 构造搜索条件
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content")) // 查询字段
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC)) // 3个排序条件，从上到下优先级依次降低
                .withPageable(PageRequest.of(0, 10)) // 分页条件
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>") // 高亮字段(实际上是添加标签)
                ).build();

        // 将查询的结果交给 SearchResultMapper 的实现类去进一步处理
        Page<DiscussPost> page = elasticTemplate.queryForPage(searchQuery, DiscussPost.class, new SearchResultMapper() {
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> aClass, Pageable pageable) {

                SearchHits hits = response.getHits();

                if (hits.getTotalHits() <= 0) {
                    return null;
                }

                List<DiscussPost> list = new ArrayList<>();
                for (SearchHit hit : hits) {

                    DiscussPost post = new DiscussPost();

                    String id = hit.getSourceAsMap().get("id").toString();
                    post.setId(Integer.parseInt(id));

                    String userId = hit.getSourceAsMap().get("userId").toString();
                    post.setUserId(Integer.parseInt(userId));

                    String title = hit.getSourceAsMap().get("title").toString();
                    post.setTitle(title);

                    String content = hit.getSourceAsMap().get("content").toString();
                    post.setContent(content);

                    String status = hit.getSourceAsMap().get("status").toString();
                    post.setStatus(Integer.parseInt(status));

                    String createTime = hit.getSourceAsMap().get("createTime").toString();
                    post.setCreateTime(new Date(Long.parseLong(createTime)));

                    String commentCount = hit.getSourceAsMap().get("commentCount").toString();
                    post.setCommentCount(Integer.parseInt(commentCount));

                    // 处理高亮显示的结果
                    HighlightField titleField = hit.getHighlightFields().get("title");
                    if (titleField != null) {
                        post.setTitle(titleField.getFragments()[0].toString());
                    }

                    HighlightField contentField = hit.getHighlightFields().get("content");
                    if (contentField != null) {
                        post.setContent(contentField.getFragments()[0].toString());
                    }

                    list.add(post);
                }

                return new AggregatedPageImpl(list,
                        pageable,
                        hits.getTotalHits(),
                        response.getAggregations(),
                        response.getScrollId(),
                        hits.getMaxScore());
            }
        });

        System.out.println(page.getTotalElements()); // 元素总数
        System.out.println(page.getTotalPages()); // 分页总数
        System.out.println(page.getNumber()); // 当前页面
        System.out.println(page.getSize()); // 每页大小
        for (DiscussPost post : page) {
            System.out.println(post);
        }
    }
}
