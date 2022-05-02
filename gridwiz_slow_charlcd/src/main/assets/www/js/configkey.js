"use strict";

function getSelectMetering(keyname, value) {
    var listVal = ["Current.Import", "Current.Offered", "Energy.Active.Import.Register", "Energy.Active.Import.Interval", "Power.Active.Import", "Power.Offered", "SoC", "Voltage"];

    var strSelect = '<select class="multi-select" multiple="multiple" id="'+keyname+'">';
    for ( var i=0; i<listVal.length; i++) {
        var selected = "";
        if (value.includes(listVal[i])) {
            selected = "selected";
        }  
        strSelect +='<option '+selected+'>'+listVal[i]+'</option>';
    }
    
    strSelect += '</select>';
    return strSelect;
}

function isKeyMeteringVal(keyname) {
    var listKey = ["MeterValuesSampledData", "MeterValuesAlignedData", "StopTxnSampledData", "StopTxnAlignedData"];
    
    for ( var i=0; i<listKey.length; i++) {
        if ( keyname == listKey[i]) {            
            return true;
        }
    }       

    return false;
}

function getConfigKey() {    
    if ( $.cookie('auth_info') == undefined ) {
        doLogout();
        return;
    }

    $.getJSON(Global.serverAddr+"/api/getconfigkey/"+$.cookie('auth_info'))
    .done(function(data) {
        console.log(data);              
        if (data.result == "success") {
            var list = data.configurationKey;
            list.forEach(element => {
                var isBool = element.value == "true" || element.value == "false";
                var strTable = '<tr><td>' + element.key+'</td><td>';
                var disabled = element.readonly ? 'disabled' : '';
                if ( isBool ) {
                        var selectedTrue = element.value == "true" ? "selected" : "";
                        var selectedFalse = element.value == "false" ? "selected" : "";

                        strTable += '<select class="form-control" id="'+element.key+'" '+disabled+'> \
                        <option value="true" '+ selectedTrue + '>True</option> \
                        <option value="false" '+ selectedFalse + '>False</option> \
                        </select>';
                }
                else if ( isKeyMeteringVal(element.key) ) {
                    strTable += getSelectMetering(element.key, element.value);
                }
                else {
                    strTable += '<input class="form-control" id="'+element.key+'" type="text" value="'+element.value+'"  '+disabled+'/>';
                }
                if ( element.readonly ) {
                    strTable += '</td><td>ReadOnly</td>';
                }
                else {
                    strTable += '</td><td><button class="btn btn-pill btn-block btn-success" type="button" value="'+element.key+'">Apply</button></td>';
                }
                

                $('#table_configkey > tbody:last').append(strTable);
            });
        }
        else {
            doLogout();
        }
        $('.multi-select').multiselect({ buttonWidth: '100%',buttonClass:'btn btn-secondary', numberDisplayed:2});
    });
}


function setConfigKey(key, value) {
    var postData = {
        token: $.cookie('auth_info'),
        key : key,
        value : value
    };
    console.log(postData);

    $.ajax({url: Global.serverAddr+"/api/setconfigkey/",
            dataType: 'json',
            type: 'post',
            contentType: 'application/json',
            data: JSON.stringify(postData),
            processData: false,
            success : function(data) {
                var resp  = data;
                console.log(resp);
                if (resp.result == "success") {
                    alert("Configuration Key changed successfully.");
                }
                else {
                    alert("Failed to change configuration key.");
                }
          },
          error : function( jqXhr, textStatus, errorThrown ){
                          console.log( errorThrown );
          }
    });
}

$('#table_configkey').on('click','button', function(evt) {    
    if ( evt.target.innerHTML != "Apply" ) return;
    console.log(evt.target.value);     
    var value = "";

    if ( isKeyMeteringVal(evt.target.value) ) {
        var arrVal =  $('#'+evt.target.value).val();
        for (var i = 0; i<arrVal.length; i++) {
            value += arrVal[i];
            if ( i != (arrVal.length -1) ) {
                value += ", ";
            }
        }
    }
    else {
        value = $('#'+evt.target.value).val();
    }
    setConfigKey(evt.target.value, value);

});

getConfigKey();

