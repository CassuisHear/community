package com.whut.community.dao.elasticsearch;

import com.whut.community.entity.DiscussPost;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository("discussPostRepository")
public interface DiscussPostRepository extends ElasticsearchRepository<DiscussPost, Integer> {
}
