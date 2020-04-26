SELECT DISTINCT * FROM public."ServerScan"
INNER JOIN public."HttpServer" ON public."ServerScan".http_server_hostname=public."HttpServer".hostname;