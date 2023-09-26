$(function(){
	$(".follow-btn").click(follow);
});

function follow() {
	var btn = this;
	if($(btn).hasClass("btn-info")) {
		// 关注TA
		// 发送异步请求
		$.ajax({
			url: CONTEXT_PATH+"/follow",
			cache: "false",
			async: "true",
			data: {
				// 实体类型 - 用户
				"entityType": 3,
				// 用户 id 从隐藏域中找到
				"entityId": $("#entityId").val()
			},
			type: "post",
			success: function (data) {
				data = $.parseJSON(data);
				if (data.code === 0) {
					// 成功则刷新页面
					window.location.reload();
				} else {
					// 失败则给出提示
					alert("关注失败...");
				}
			}
		});
		//$(btn).text("已关注").removeClass("btn-info").addClass("btn-secondary");
	} else {
		// 取消关注
		$.ajax({
			url: CONTEXT_PATH+"/unFollow",
			cache: "false",
			async: "true",
			data: {
				// 实体类型 - 用户
				"entityType": 3,
				// 用户 id 从隐藏域中找到
				"entityId": $("#entityId").val()
			},
			type: "post",
			success: function (data) {
				data = $.parseJSON(data);
				if (data.code === 0) {
					// 成功则刷新页面
					window.location.reload();
				} else {
					// 失败则给出提示
					alert("取消关注失败...");
				}
			}
		});
		//$(btn).text("关注TA").removeClass("btn-secondary").addClass("btn-info");
	}
}