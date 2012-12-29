package smtpclient;
/**
 *
 * @author Predrag
 */
public class ContentType {
    
    public static String extensionCheck(String extension, String attachmentPath) {
        
        // Ubacujem sve ekstenzije za slike
        if(extension.equals("jpg") || 
           extension.equals("gif") ||
           extension.equals("jpeg") ||     
           extension.equals("png") ||
           extension.equals("pjpeg") || 
           extension.equals("tiff") ||
           extension.equals("gif") || 
           extension.equals("png")     
           )
         return ("Content-Type:image/"+extension+";" + "name="+attachmentPath);
        
        
        // Ubacujem sve ekstenzije za poruke
        if(extension.equals("http") || 
           extension.equals("rfc822") || 
           extension.equals("partial")   
           )
         return "Content-Type:message/"+extension+";" + "name="+attachmentPath;        
        
        
        // Ubacujem sve ekstenzije za audio
        if(extension.equals("basic") || 
           extension.equals("mp4") ||
           extension.equals("mpeg") ||     
           extension.equals("ogg") ||
           extension.equals("webm") || 
           extension.equals("vorbis")    
           )
         return "Content-Type:audio/"+extension+";" + "name="+attachmentPath;        
        
        
        // Ubacujem ekstenzije za application
        if(extension.equals("zip") || 
           extension.equals("rar") ||
           extension.equals("javascript") ||     
           extension.equals("json") ||
           extension.equals("pdf") || 
           extension.equals("postscript") || 
           extension.equals("gzip") ||     
           extension.equals("xml") ||
           extension.equals("rar") || 
           extension.equals("doc") ||
           extension.equals("docx") || 
           extension.equals("xls") ||     
           extension.equals("ppt") ||
           extension.equals("pptx") || 
           extension.equals("potx")                 
           )
         return "Content-Type:application/"+extension+"; name="+attachmentPath;
        
        // Ubacujem ekstenzije za text
        if(extension.equals("cmd") || 
           extension.equals("css") ||
           extension.equals("csv") ||     
           extension.equals("html") ||
           extension.equals("javascript") || 
           extension.equals("plain") || 
           extension.equals("vcard") ||     
           extension.equals("xml")               
           )
         return "Content-Type:text/"+extension+"; name="+attachmentPath;
        
        return "Fajl ne postoji";
    }
    
}
