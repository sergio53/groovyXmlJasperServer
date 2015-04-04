import com.sun.net.httpserver.*
import net.sf.jasperreports.engine.*
import net.sf.jasperreports.engine.export.*

def old_StdOut = System.out
def root_Dir= new File(".").getCanonicalPath()

def port = this.args ? this.args[0].toInteger() : 8800
def serv = HttpServer.create(new InetSocketAddress(port),0)

serv.with {
  HttpContext kill = createContext("/kill", new Kill(server:serv))
  kill.setAuthenticator(new BasicAuthenticator("kill") {
    public boolean checkCredentials(String user, String pwd) {
      println "user==$user pwd==$pwd"
      "$user$pwd"=="is10-99,jhbcsx"
    }
  });
  createContext("/", new reportsMaker(rootDir:root_Dir,oldStdOut:old_StdOut))  
  setExecutor(null)
  start()
}
class Kill implements HttpHandler {
  def server
  public void handle(HttpExchange exchange) {
    def path = exchange.getRequestURI().getPath()
    //println "\nkill::==> ${path}"
    exchange.responseHeaders['Content-Type'] = 'text/html; charset=UTF-8'
    exchange.sendResponseHeaders(200, 0)
    if(path!="/kill") { exchange.close(); return }
  
    def out = new PrintWriter(exchange.getResponseBody())
    out.println "<H1>Groovy-JRServer killed.</H1>"
    out.close()
    exchange.close()
    server.stop(3)
    println "\nGroovy-JRServer killed.\n-------------------------------"
  }
}


class reportsMaker implements HttpHandler {
  def rootDir
  def oldStdOut
  public void handle(HttpExchange exchange) throws IOException {
    System.out = oldStdOut
    def h = exchange.getResponseHeaders()
    //=>позиционые/параметры?именованные=параметры
    def path = exchange.getRequestURI().getPath().tokenize("/")// b/e/f/o/r/e?
    def query = exchange.getRequestURI().getQuery()// ?a=f&t=er
    println "\n.......................\n${path}?${query ?:''}"
    if(path.size()<4){
      h.add('Content-Type', 'text/html; charset=UTF-8')
      exchange.sendResponseHeaders(200, 0)
      exchange.close()
      return
    }
    
    def hmap = [:]
    if(query) query.tokenize('&').each {
      def xx=it.tokenize('=')
      hmap[xx[0]]= xx[1]
    }
    
    def jasperFile = "${rootDir}/jasper/${path[1]}.jasper"
    def jasperReport = net.sf.jasperreports.engine.util.JRLoader.loadObject(new File(jasperFile))
    def queryText = jasperReport.getQuery().getText()
    def defaults = net.sf.jasperreports.engine.fill.JRParameterDefaultValuesEvaluator.
      evaluateParameterDefaultValues(jasperReport, new HashMap())
    
    def par, XMLrequest = new StringBuffer()
    defaults.each {k,v->
      par = ""
      if(k.startsWith("XML_POST_PARAM_")) par= k[15..-1]
      if(k.startsWith("XML_GET_PARAM_")) par= k[14..-1]
      if(par!=""){
        if(hmap[par]) XMLrequest<<par<<"="<<hmap[par]<<"&"
        else XMLrequest<<par<<"="<<v<<"&" // defaults
      }
    }
    
    def bufStr = new ByteArrayOutputStream()
    if(path[2]=="groovy") {
      //def groovyFile = "${rootDir}/groovy/${path[3]}.groovy"
      String groovyFile = "${rootDir}/groovy/${path[3]}.groovy"
      println "$groovyFile $XMLrequest"
      hmap['XML_URL'] = groovyFile
      System.out = new PrintStream(bufStr)
      new GroovyShell().run(new File(groovyFile), "$XMLrequest")
    }
		//if(path[2]=="jar")
    System.out = oldStdOut
    
    def xml= "$bufStr"
    
    def doc=net.sf.jasperreports.engine.util.JRXmlUtils.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")))
    def con = new net.sf.jasperreports.engine.data.JRXmlDataSource(doc,queryText)
    def jasperPrint = JasperFillManager.fillReport(jasperFile, hmap, con)
    
    //export:
    def exporter, exporterParameter
    switch(path[0]){
    case "html":
      exporter= new JRHtmlExporter()
      exporterParameter= new JRHtmlExporterParameter()
      exporter.setParameter(exporterParameter.IS_USING_IMAGES_TO_ALIGN, false)
      h.add("Content-Type", "text/html; charset=UTF-8")
      break
    case "rtf":
      exporter= new JRRtfExporter()
      exporterParameter= new JRExporterParameter()
      h.add("Content-Type", "application/msword")
      break
    case "xls":
      exporter= new JExcelApiExporter()
      exporterParameter= new JRXlsExporterParameter()
      exporter.setParameter(exporterParameter.IS_DETECT_CELL_TYPE, false)
      //...
      exporter.setParameter(exporterParameter.IS_WHITE_PAGE_BACKGROUND, false)
      exporter.setParameter(exporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_ROWS, true)
      h.add("Content-Type", "application/vnd.ms-excel")
      break
    default: //.pdf
      exporter= new JRPdfExporter()
      exporterParameter= new JRPdfExporterParameter()
      h.add("Content-Type", "application/pdf")
    }
    //
    def out = new java.io.ByteArrayOutputStream()
    exporter.setParameter(exporterParameter.JASPER_PRINT,jasperPrint)
    exporter.setParameter(exporterParameter.OUTPUT_STREAM, out)

    exporter.exportReport()

    h.add("Content-Disposition", "inline; filename=${path[1]}")
    exchange.sendResponseHeaders(200, out.size())
    def os = exchange.getResponseBody()
    os.write(out.toByteArray())
    os.close
    println "----------------------"
    exchange.close()
  }
}

println "\nGroovy-JRServer:$port startded in directory $root_Dir"
//
java.awt.Desktop.getDesktop().browse(new URI("http://localhost:8800/html/binlogHTTP/groovy/binlog?pCode=1RH31S101XG01&pStart=200910061523&pStop=201103281305"))