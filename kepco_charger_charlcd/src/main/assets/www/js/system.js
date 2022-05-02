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
            $("#server_addr").val(system.server_addr);
            $("#server_port").val(system.server_port);
            $("#auth_key").val(system.auth_key);
            $("#charger_id").val(system.charger_id);            
            $("#admin_pwd").val(system.admin_pwd);
            if (system.use_payterminal == "true" ) {
                $("#use_payterminal").attr("checked","");
            }
            else {
                $("#use_payterminal").removeAttr("checked");
            }
            if (system.use_watchdog == "true" ) {
                $("#use_watchdog").attr("checked","");
            }
            else {
                $("#use_watchdog").removeAttr("checked");
            }            
            if (system.auth_skip == "true" ) {            
                $("#auth_skip").attr("checked","");
            }
            else {
                $("#auth_skip").removeAttr("checked");
            }            
        }
        else {
            doLogout();
        }        
    });
}


$("#submit").click(function() {
    if ( $("#auth_key").val().length == 0) {
        $("#wrongMsg").text("Auth Key is empty.");
        return;
    }

    if ( $("#charger_id").val().length == 0) {
        $("#wrongMsg").text("Charger ID is emtpy.");
        return;
    }

    if (!confirm('Reboot is required. Are you sure you want to save this setting')) {
        return;
    }
    
    var postData = {
        token: $.cookie('auth_info'),
        server_addr : $("#server_addr").val(),
        server_port : $("#server_port").val(),
        auth_key : $("#auth_key").val(),
        charger_id : $("#charger_id").val(),        
        admin_pwd: $("#admin_pwd").val(),
        use_payterminal: $("#use_payterminal").is(":checked"),
        use_watchdog: $("#use_watchdog").is(":checked"),
        auth_skip: $("#auth_skip").is(":checked"),
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