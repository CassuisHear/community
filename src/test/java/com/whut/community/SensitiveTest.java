package com.whut.community;

import com.whut.community.util.SensitiveFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
class SensitiveTest {

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Test
    void testSensitiveFilter() {
        String text1 = "dsjifjia※傻※逼※, jisfa※操※你※妈※ isjduwe*232※开※票※ ji";
        String text2 = " iajaihewi*※嫖※娼 jisaifwef※吸※毒※ii iw可以 is ※赌※博※ jiij笨蛋※";
        String ans1 = sensitiveFilter.filter(text1);
        String ans2 = sensitiveFilter.filter(text2);
        System.out.println("ans = " + ans1);
        System.out.println("ans2 = " + ans2);
    }
}
