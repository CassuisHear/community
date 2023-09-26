$(function(){
	$("#sendBtn").click(send_letter);
	$(".close").click(delete_msg);
});

function send_letter() {
	$("#sendModal").modal("hide");

	// 获取发送目标的名字和内容
	let toName = $("#recipient-name").val();
	let content = $("#message-text").val();
	// 发送异步请求
	$.ajax({
		url: CONTEXT_PATH+"/letter/send",
		cache: "false",
		async: "true",
		data: {
			"toName": toName,
			"content": content
		},
		type: "post",
		success: function(data) {
			data = $.parseJSON(data);

			// 发送成功
			if (data.code === 0) {
				$("#hintBody").text("发送成功！");
			} else {
				$("#hintBody").text("发送失败... " + data.msg);
			}

			$("#hintModal").modal("show");
			setTimeout(function(){
				$("#hintModal").modal("hide");
				// 成功时重新加载页面
				if (data.code === 0) {
					window.location.reload();
				}
			}, 2000);
		}
	});
}

function delete_msg() {
	// TODO 删除数据
	$(this).parents(".media").remove();
}