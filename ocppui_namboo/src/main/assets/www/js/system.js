"use strict";

function getSystemInfo() {    
    if ( $.cookie('auth_info') == undefined ) {
        doLogout();
        return;
    }

    $.getJSON(Global.serverAddr+"/api/getsystem/"+$.cookie('auth_info'))
    .done(function(data) {
        console.log(data);              
        if (data.result == "success") {        
            var system = data.system;
            console.log(system); 
            $("#charger_id").val(system.charger_id);
            $("#server_uri").val(system.server_uri);
            $("#http_auth_id").val(system.http_auth_id);
            $("#http_auth_pwd").val(system.http_auth_pwd);
            $("#admin_pwd").val(system.admin_pwd);
            if (system.use_http_auth == "true" ) {
                $("#use_http_auth").attr("checked","");
            }
            else {
                $("#use_http_auth").removeAttr("checked");
            }
            if (system.use_watchdog == "true" ) {            
                $("#use_watchdog").attr("checked","");
            }
            else {
                $("#use_watchdog").removeAttr("checked");
            }
            if(system.is_fastcharger == "true"){
                $("#is_fastcharger").attr("checked","");
            }
            else{
                $("#is_fastcharger").removeAttr("checked");
            }
            if (system.use_trustca == "true" ) {
                $("#use_trustca").attr("checked","");
            }
            else {
                $("#use_trustca").removeAttr("checked");
            }
        }
        else {
            doLogout();
        }        
    });
}


$("#submit").click(function() {
    if (!confirm('Reboot is required. Are you sure you want to save this setting')) {
        return;
    }

    var postData = {
        token: $.cookie('auth_info'),
        charger_id : $("#charger_id").val(),
        server_uri : $("#server_uri").val(),
        http_auth_id : $("#http_auth_id").val(),
        http_auth_pwd: $("#http_auth_pwd").val(),
        admin_pwd: $("#admin_pwd").val(),
        use_http_auth: $("#use_http_auth").is(":checked"),
        use_watchdog: $("#use_watchdog").is(":checked"),
        use_trustca: $("#use_trustca").is(":checked"),
        is_fastcharger: $("#is_fastcharger").is(":checked")
    };

    $.ajax({url: Global.serverAddr+"/api/setsystem/",
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

$("#btStartUpdate").click(function() {
    if ($('#file-input').val() == "") {
        alert("File not found. Please select update file.");
        return;
    }
    if (!confirm('Reboot is required. Are you sure you want to update?')) {
        return;
    }

    var datas, xhr;
     
    datas = new FormData();
    datas.append('file', $('#file-input')[0].files[0] );
    datas.append('token', $.cookie('auth_info'));

    $.ajax({
        url: Global.serverAddr+"/api/swupdate",
        contentType: 'multipart/form-data', 
        type: 'POST',
        data: datas,   
        dataType: 'json',     
        mimeType: 'multipart/form-data',
        success: function (data) {               
             //alert( data.url );
              alert("The file upload is complete. After 15 sec, S/W update and reset.");
        },
        error : function (jqXHR, textStatus, errorThrown) {
            alert('ERRORS: ' + textStatus);
        },
        cache: false,
        contentType: false,
        processData: false
    });       
});


$("#btStartUploadCA").click(function() {
    if ($('#file-ca').val() == "") {
        alert("File not found. Please select update file.");
        return;
    }

    var datas, xhr;

    datas = new FormData();
    datas.append('file', $('#file-ca')[0].files[0] );
    datas.append('token', $.cookie('auth_info'));

    $.ajax({
        url: Global.serverAddr+"/api/uploadca",
        contentType: 'multipart/form-data',
        type: 'POST',
        data: datas,
        dataType: 'json',
        mimeType: 'multipart/form-data',
        success: function (data) {
              alert("The file upload is complete. After 5 sec, application will be restart.");
        },
        error : function (jqXHR, textStatus, errorThrown) {
            alert('ERRORS: ' + textStatus);
        },
        cache: false,
        contentType: false,
        processData: false
    });
});

$("#btFactoryReset").click(function() {
    if (!confirm('All Data will be erased. Do you sill want do it?')) {
       return;
    }

    if ( $.cookie('auth_info') == undefined ) {
        doLogout();
        return;
    }

    $.getJSON(Global.serverAddr+"/api/factoryreset/"+$.cookie('auth_info'))
    .done(function(data) {
        console.log(data);
        if (data.result == "success") {
            var system = data.system;
            alert("Factory reset is complete. After 15 sec, system will be reboot.");
        }
        else {
            doLogout();
        }
    });

});

$("#reset").click(function() {
    getSystemInfo();
    $("#wrongMsg").text("");
});


getSystemInfo();