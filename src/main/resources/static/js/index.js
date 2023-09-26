$(function(){
	$("#publishBtn").click(publish);
});

function publish() {
	// 弹出框隐藏
	$("#publishModal").modal("hide");

	// // 发送 AJAX 请求之前，将 CSRF 令牌设置到请求的消息头中
	// let token = $("meta[name='_csrf']").attr("content");
	// let header = $("meta[name='_csrf_header']").attr("content");
	// $(document).ajaxSend(function (e, xhr, options) {
	// 	xhr.setRequestHeader(header, token);
	// });

	// 获取标题和内容
	let title = $("#recipient-name").val();
	let content = $("#message-text").val();
	// 发送异步请求
	$.ajax({
		url: CONTEXT_PATH+"/discuss/add",
		cache: "false",
		async: "true",
		data: {
			"title": title,
			"content": content
		},
		type: "post",
		success: function(data) {
			data = $.parseJSON(data);
			// 在提示框中显示返回信息
			$("#hintBody").text(data.msg);

			// 提示框显示，2秒后自动隐藏
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