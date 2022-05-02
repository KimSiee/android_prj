"use strict";

function getLocalAuthList() {    
    if ( $.cookie('auth_info') == undefined ) {
        doLogout();
        return;
    }

    $.getJSON(Global.serverAddr+"/api/getlocalauth/"+$.cookie('auth_info'))
    .done(function(data) {
        //console.log(data);              
        if (data.result == "success") {
            var list = data.list;                   
            list.forEach(element =>  {                
                var strTable = '<tr><td>'+element.idtag+'</td><td>'+element.parentid+'</td><td>'+element.status+'</td><td>'+element.expired+"</td></tr>";          
                $('#tableSysLog > tbody:last').append(strTable);
            });
        }
        else {
            doLogout();
        }
        
    });
}

$("#btClearLocalAuth").click(function() {
    if (!confirm('All Local Auth Data will be erased. Do you sill want do it?')) {
       return;
    }

    if ( $.cookie('auth_info') == undefined ) {
        doLogout();
        return;
    }

    $.getJSON(Global.serverAddr+"/api/clearlocalauth/"+$.cookie('auth_info'))
    .done(function(data) {
        console.log(data);
        if (data.result == "success") {
            var system = data.system;
            alert("Clear Local Auth is complete.");
        }
        else {
            doLogout();
        }
    });

});

getLocalAuthList();
