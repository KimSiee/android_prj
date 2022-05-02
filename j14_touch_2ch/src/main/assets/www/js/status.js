"use strict";

function getStatus() {    
    if ( $.cookie('auth_info') == undefined ) {
        doLogout();
        return;
    }

    $.getJSON(Global.serverAddr+"/api/getstatus/"+$.cookie('auth_info'))
    .done(function(data) {
        console.log(data);              
        if (data.result == "success") {
        var info = data.version;
        $("#model_name").text(info.model_name);
        $("#sw_ver").text(info.sw_ver);
        $("#dsp_ver").text(info.dsp_ver);            

        var net = data.network;
        $("#net_type").text(net.type);
        $("#net_ip").text(net.ip);
        $("#net_mask").text(net.netmask);
        $("#net_gw").text(net.gateway);
        $("#net_dns").text(net.dns);

        var cs = data.charger;
        $("#cs_svr").text(cs.svraddr);
        $("#cs_id").text(cs.cid);
        $("#cs_comm").text(cs.comm);
        $("#cs_connector").text(cs.connector);
        
        }
        else {
            doLogout();
        }
        
    });
}

getStatus();
