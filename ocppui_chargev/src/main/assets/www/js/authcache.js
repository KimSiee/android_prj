"use strict";

function getAuthCacheList() {    
    if ( $.cookie('auth_info') == undefined ) {
        doLogout();
        return;
    }

    $.getJSON(Global.serverAddr+"/api/getauthcache/"+$.cookie('auth_info'))
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

$("#btClearCache").click(function() {
    if (!confirm('All Cache Data will be erased. Do you sill want do it?')) {
       return;
    }

    if ( $.cookie('auth_info') == undefined ) {
        doLogout();
        return;
    }

    $.getJSON(Global.serverAddr+"/api/clearcache/"+$.cookie('auth_info'))
    .done(function(data) {
        console.log(data);
        if (data.result == "success") {
            var system = data.system;
            alert("Clear cache is complete.");
        }
        else {
            doLogout();
        }
    });

});

getAuthCacheList();
