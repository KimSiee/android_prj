"use strict";

function getNetworkStatus() {    
    if ( $.cookie('auth_info') == undefined ) {
        doLogout();
        return;
    }

    $.getJSON(Global.serverAddr+"/api/getnetwork/"+$.cookie('auth_info'))
    .done(function(data) {
        console.log(data);              
        if (data.result == "success") {
            var net = data.network;
            if ( net.type == "DHCP" ) $("#selectType").val("dhcp");
            else  $("#selectType").val("static");
            $("#ipaddr").val(net.ip);
            $("#netmask").val(net.netmask);
            $("#gateway").val(net.gateway);
            $("#dns").val(net.dns);
            onChangeType();
        }
        else {
            doLogout();
        }        
    });
}

function setNetworkAddress() {
    var postData = {
        token: $.cookie('auth_info'),
        type : $("#selectType").val(),
        ipaddr : $("#ipaddr").val(),
        netmask : $("#netmask").val(),
        gateway: $("#gateway").val(),
        dns: $("#dns").val()
    };

    $.ajax({url: Global.serverAddr+"/api/setnetwork/",
            dataType: 'json',
            type: 'post',
            contentType: 'application/json',
            data: JSON.stringify(postData),
            processData: false,
            success : function(data) {
                var resp  = data;
                console.log(resp);
                if (resp.result == "success") {
                    $("#wrongMsg").text("Network Setting was changed successfully. After 15 seconds the system will reboot.");
                }
                else {
                    $("#wrongMsg").text("Failed to change network settings.");
                }
          },
          error : function( jqXhr, textStatus, errorThrown ){
                          console.log( errorThrown );
          }
    });
}

function onChangeType() {
    var val = $("#selectType option:selected").val();
    if ( val == "dhcp" ) {
        $("#ipaddr").attr("disabled","");
        $("#netmask").attr("disabled","");
        $("#gateway").attr("disabled","");
        $("#dns").attr("disabled","");
    } 
    else {
        $("#ipaddr").removeAttr("disabled");
        $("#netmask").removeAttr("disabled");
        $("#gateway").removeAttr("disabled");
        $("#dns").removeAttr("disabled");
    }
}

$("#selectType").change(function() {
    onChangeType();    
});

$("#submit").click(function() {
    if (!confirm('Reboot is required. Are you sure you want to save this setting')) {
        return;
    }
    var val = $("#selectType option:selected").val();
    if ( val != "dhcp" ) {
        if ( ValidateIPaddress($("#ipaddr").val()) == false) {
            $("#wrongMsg").text("You have entered an invalid IP address!");
            return;
        }

        if ( ValidateIPaddress($("#netmask").val()) == false) {
            $("#wrongMsg").text("You have entered an invalid Netmask!");
            return;
        }

        if ( ValidateIPaddress($("#gateway").val()) == false) {
            $("#wrongMsg").text("You have entered an invalid Gateway!");
            return;
        }

        if ( ValidateIPaddress($("#dns").val()) == false) {
            $("#wrongMsg").text("You have entered an invalid DNS!");
            return;
        }
    }
    $("#wrongMsg").text("");

    setNetworkAddress();
});

$("#reset").click(function() {
    getNetworkStatus();
    $("#wrongMsg").text("");
});

getNetworkStatus();