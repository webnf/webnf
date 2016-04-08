/** @constructor */
var JSZip = {
    file : function(){},
    folder : function(){},
    filter : function(){},
    remove : function(){},
    generate : function(){},
    load : function(){},
    support : {},
    files : [],
    comment : ""
};
    
var ZipObject = {
    asText : function(){},
    asBinary : function(){},
    asArrayBuffer : function(){},
    asUint8Array : function(){},
    asNodeBuffer : function(){},
    name : {},
    dir : {},
    date : {},
    comment : {},
    unixPermissions : {},
    dosPermissions : {},
    options : {
        base64 : {},
        binary : {},
        dir : {},
        date : {},
        compression : {}
    }
};
