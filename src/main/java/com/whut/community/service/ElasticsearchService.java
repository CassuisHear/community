package com.whut.community.service;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ElasticsearchService {

    private DiscussPostRepository discussPostRepository;

    private ElasticsearchTemplate elasticTemplate;

    public ElasticsearchService(@Qualifier("discussPostRepository") DiscussPostRepository discussPostRepository,
                                @Autowired ElasticsearchTemplate elasticTemplate) {
        this.discussPostRepository = discussPostRepository;
        this.elasticTemplate = elasticTemplate;
    }

    // 向 ES 中存储/修改一个帖子，使用 DiscussPostRepository
    public void saveDiscussPost(DiscussPost discussPost) {
        discussPostRepository.save(discussPost);
    }

    // 在 ES 中删除一个帖子，使用 DiscussPostRepository
    public void deleteDiscussPost(int postId) {
        discussPostRepository.deleteById(postId);
    }

    // 在 ES 中根据一个关键词查询多个帖子，使用 ElasticsearchTemplate
    public Page<DiscussPost> searchDiscussPost(String keyWord, int current, int limit) {
        // 1.构造搜索条件
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery(keyWord, "title", "content")) // 查询字段
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC)) // 3个排序条件，从上到下优先级依次降低
                .withPageable(PageRequest.of(current, limit)) // 分页条件，当前页码和每页大小
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>") // 高亮字段(实际上是添加标签)
                ).build();

        // 2.将查询的结果交给 SearchResultMapper 的实现类去进一步处理(前后添加标签)
        return elasticTemplate.queryForPage(searchQuery, DiscussPost.class, new SearchResultMapper() {
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> aClass, Pageable pageable) {

                // 2.1获取命中数据
                SearchHits hits = response.getHits();
                if (hits.getTotalHits() <= 0) {
                    return null;
                }

                // 2.2根据命中数据对结果进行处理
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

                return new AggregatedPageImpl(list, pageable, hits.getTotalHits(),
                        response.getAggregations(), response.getScrollId(), hits.getMaxScore());
            }
        });
    }
}
