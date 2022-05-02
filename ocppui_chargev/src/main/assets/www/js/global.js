var Global = {    
    serverAddr : "http://"+location.host
};

function doLogout() {
    $.removeCookie('auth_info');
    window.location.href = "/login.html";  
}

function ValidateIPaddress(ipaddress) 
{
    var ipformat = /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
    if(ipaddress.match(ipformat))
    {    
        return true;
    }
    else
    {    
        return false;
    } 
}