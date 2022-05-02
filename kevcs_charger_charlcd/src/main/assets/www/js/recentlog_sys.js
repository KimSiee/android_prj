"use strict";

function getRecentSysLog() {    
    if ( $.cookie('auth_info') == undefined ) {
        doLogout();
        return;
    }

    $.getJSON(Global.serverAddr+"/api/getrecentsyslog/"+$.cookie('auth_info'))
    .done(function(data) {
        //console.log(data);              
        if (data.result == "success") {
            var list = data.log;                   
            list.forEach(element =>  {                
                var strTable = '<tr><td>'+element.time+'</td><td>'+element.level+'</td><td>'+element.tag+'</td><td style="word-break: break-all;">'+element.msg+"</td></tr>";
                $('#tableSysLog > tbody:last').append(strTable);
            });
        }
        else {
            doLogout();
        }
        
    });
}

getRecentSysLog();
