"use strict";

function getRecentCommLog() {    
    if ( $.cookie('auth_info') == undefined ) {
        doLogout();
        return;
    }

    $.getJSON(Global.serverAddr+"/api/getrecentcommlog/"+$.cookie('auth_info'))
    .done(function(data) {
        console.log(data);              
        if (data.result == "success") {
            var list = data.log;                   
            list.forEach(element =>  {                
                var strTable = '<tr><td>'+element.time+'</td><td>'+element.tag+'</td><td style="word-break: break-all;">'+element.msg+'</td></tr>';
                $('#tableCommLog > tbody:last').append(strTable);
            });
        }
        else {
            doLogout();
        }
        
    });
}

getRecentCommLog();
