$(function () {
    $("input").focus(clear_error);
    // 修改密码
    $("#form-updatePassword").submit(check_data);
    // 上传头像
    $("#uploadForm").submit(upload);
});

function check_data() {
    // 选取3个input标签
    let input_oldPassword = $("#old-password");
    let input_newPassword = $("#new-password");
    let input_confirmPassword = $("#confirm-password");

    // 获取3个input标签的值
    let oldPassword = input_oldPassword.val();
    let newPassword = input_newPassword.val();
    let confirmPassword = input_confirmPassword.val();

    // 如果新密码和旧密码相同或者两次新密码输入不一致
    if (oldPassword === newPassword || newPassword !== confirmPassword) {
        if (oldPassword === newPassword) {
            input_newPassword.addClass("is-invalid");
            return false;
        }
        if (newPassword !== confirmPassword) {
            input_confirmPassword.addClass("is-invalid");
        }
        return false;
    }
    return true;
}

// 上传头像
function upload() {
    $.ajax({
        url: "http://upload-z1.qiniup.com",
        method: "post",
        processData: false,
        contentType: false,
        data: new FormData($("#uploadForm")[0]),
        success: function (data) {
            if (data && data.code == 0) {
                // 更新头像的访问路径
                $.post(
                    CONTEXT_PATH+"/user/header/url",
                    {"fileName": $("input[name='key']").val()},
                    function(data) {
                        data = $.parseJSON(data);
                        if (data.code == 0) {
                            window.location.reload();
                        } else {
                            alert(data.msg);
                        }
                    }
                );
            } else {
                alert("上传失败!");
            }
        }
    });

    return false;
}

function clear_error() {
    $(this).removeClass("is-invalid");
}