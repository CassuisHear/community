$(function () {
   $("#topBtn").click(setTop);
   $("#wonderfulBtn").click(setWonderful);
   $("#deleteBtn").click(setDelete);
});

function like(btn, entityType, entityId, entityUserId, postId) {
    // 发送异步请求
    $.ajax({
        url: CONTEXT_PATH+"/like",
        cache: "false",
        async: "true",
        data: {
            "entityType": entityType,
            "entityId": entityId,
            "entityUserId": entityUserId,
            "postId": postId
        },
        type: "post",
        success: function(data) {
            // 转化为 JSON 对象
            data = $.parseJSON(data);
            // 请求成功
            if (data.code === 0) {
                // 得到对应的 a 标签对象
                $(btn).children("b").text(data.likeStatus==1?'已赞':'赞');
                $(btn).children("i").text(data.likeCount);
            } else { // 请求失败
                alert(data.msg);
            }
        }
    });
}

// 置顶和取消置顶
function setTop() {
    $.post(
        CONTEXT_PATH+"/discuss/top",
        {"id":$("#postId").val()},
        function (data) {
            data = $.parseJSON(data);
            if (data.code === 0) {
                $("#topBtn").text(data.type===1?'取消置顶':'置顶');
                alert(data.msg);
            } else {
                alert(data.msg);
            }
        }
    );
}

// 加精和取消加精
function setWonderful() {
    $.post(
        CONTEXT_PATH+"/discuss/wonderful",
        {"id":$("#postId").val()},
        function (data) {
            data = $.parseJSON(data);
            if (data.code === 0) {
                $("#wonderfulBtn").text(data.status===1?'取消加精':'加精');
                alert(data.msg);
            } else {
                alert(data.msg);
            }
        }
    );
}

// 删除
function setDelete() {
    $.post(
        CONTEXT_PATH+"/discuss/delete",
        {"id":$("#postId").val()},
        function (data) {
            data = $.parseJSON(data);
            if (data.code === 0) {
                // 这个帖子被删除后直接跳转到首页
                location.href = CONTEXT_PATH+"/index";
            } else {
                alert(data.msg);
            }
        }
    );
}