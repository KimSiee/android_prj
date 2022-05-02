"use strict";

function getCommLogFiles() {    
    if ( $.cookie('auth_info') == undefined ) {
        doLogout();
        return;
    }

    $.getJSON(Global.serverAddr+"/api/getcommlogfiles/"+$.cookie('auth_info'))
    .done(function(data) {
        console.log(data);              
        if (data.result == "success") {
            var list = data.files;                   
            list.forEach(element =>  {                
                var strTable = '<tr><td><a href="#" onclick="downFile(\''+element+'\')">'+element+'</a></td></tr>';
                $('#tableCommLog > tbody:last').append(strTable);
            });
        }
        else {
            doLogout();
        }
        
    });
}

function downFile(filename) {
    if ( $.cookie('auth_info') == undefined ) {
        doLogout();
        return;
    }

    window.open(Global.serverAddr+"/api/getcommlogfiledown/"+$.cookie('auth_info')+"/"+filename, "_blank");
}

getCommLogFiles();
