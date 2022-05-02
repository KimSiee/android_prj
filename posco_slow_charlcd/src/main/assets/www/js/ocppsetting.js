"use strict";

function getOcppSystemInfo() {
    if ( $.cookie('auth_info') == undefined ) {
        doLogout();
        return;
    }

    $.getJSON(Global.serverAddr+"/api/getocppsetting/"+$.cookie('auth_info'))
        .done(function(data) {
            console.log(data);
            if (data.result == "success") {
                var ocppsetting = data.ocppsetting;
                console.log(ocppsetting);
                $("#ocpp_server_url").val(ocppsetting.ocpp_server_url);
                $("#ocpp_cpid").val(ocppsetting.ocpp_cpid);
                $("#auth_id").val(ocppsetting.auth_id);
                $("#auth_password").val(ocppsetting.auth_password);
                $("#charger_serial_num").val(ocppsetting.charger_serial_num);
                $("#use_ocpp").val(ocppsetting.use_ocpp);
                $("#use_ssl").val(ocppsetting.use_ssl);
                $("#use_basic_auth").val(ocppsetting.use_basic_auth);

                if (ocppsetting.use_ocpp == "true" ) {
                    $("#use_ocpp").attr("checked","");
                }
                else {
                    $("#use_ocpp").removeAttr("checked");
                }
                if (ocppsetting.use_ssl == "true" ) {
                    $("#use_ssl").attr("checked","");
                }
                else {
                    $("#use_ssl").removeAttr("checked");
                }
                if (ocppsetting.use_basic_auth == "true" ) {
                    $("#use_basic_auth").attr("checked","");
                }
                else {
                    $("#use_basic_auth").removeAttr("checked");
                }
            }
            else {
                doLogout();
            }
    });
}

$("#submit").click(function() {
    if ( $("#ocpp_server_url").val().length == 0) {
        $("#wrongMsg").text("Server URL is empty");
        return;
    }

    if($("#ocpp_cpid").val().length == 0){
        $("#wrongMsg").text("CPID is empty");
        return;
    }

    if (!confirm('Reboot is required. Are you sure you want to save this setting')) {
        return;
    }

    var postData = {
        token: $.cookie('auth_info'),
        ocpp_server_url : $("#ocpp_server_url").val(),
        ocpp_cpid : $("#ocpp_cpid").val(),
        auth_id : $("#auth_id").val(),
        auth_password : $("#auth_password").val(),
        charger_serial_num: $("#charger_serial_num").val(),
        use_ocpp: $("#use_ocpp").is(":checked"),
        use_ssl: $("#use_ssl").is(":checked"),
        use_basic_auth: $("#use_basic_auth").is(":checked"),
    };

    $.ajax({url: Global.serverAddr+"/api/setocppsetting/",
                dataType: 'json',
                type: 'post',
                contentType: 'application/json',
                data: JSON.stringify(postData),
                processData: false,
                success : function(data) {
                    var resp  = data;
                    console.log(resp);
                    if (resp.result == "success") {
                        $("#wrongMsg").text("System setting was changed successfully. After 5 seconds,  application will be restart.");
                    }
                    else {
                        $("#wrongMsg").text("Failed to change system settings.");
                    }
              },
              error : function( jqXhr, textStatus, errorThrown ){
                              console.log( errorThrown );
              }
    });
});

getOcppSystemInfo();